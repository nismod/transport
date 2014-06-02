Module RlZoneExtVarCalc1pt4
    '1.2 this version allows capacity changes to be included
    '1.2 it now allows external variable growth factors to be taken from an input file and to vary over time
    '1.3 this version allows input from the database
    'also includes fuel efficiency changes
    'now also includes electrification schemes
    '1.4 fuel efficiency calculation corrected
    'v1.6 now calculate by annual timesteps
    'correct the error that the original code doesnot read the electrificationdata correctly
    'note this module needs to run RlLinkExtVarCalc first

    Dim RlZoneInputData As IO.FileStream
    Dim rlzi As IO.StreamReader
    Dim RlZoneExtVar As IO.FileStream
    Dim rlze As IO.StreamWriter
    Dim RlZoneCapData As IO.FileStream
    Dim rlzc As IO.StreamReader
    Dim RlZoneEVScale As IO.FileStream
    Dim rlzs As IO.StreamReader
    Dim RlZoneElSchemes As IO.FileStream
    Dim rzel As IO.StreamReader
    Dim InputRow As String
    Dim OutputRow As String
    Dim PopGrowth As Double
    Dim GVAGrowth As Double
    Dim CostGrowth As Double
    Dim FuelGrowth As Double
    Dim GJTGrowth As Double
    Dim ElPGrowth As Double
    Dim InputCount As Integer
    Dim CapID As Long
    Dim CapYear As Integer
    Dim InputData() As String
    Dim StationChange As Integer
    Dim TripChange As Double
    Dim ErrorString As String
    Dim stf As IO.StreamReader
    Dim stratstring, stratarray() As String
    Dim FuelEff(1, 90), CO2Vol(1, 90), CO2Price(1, 90), GJTProp(1, 90) As Double
    Dim ElectricZone, ElectricYear, ElectricStations As Long
    Dim FuelEffOld(2) As Double
    Dim enestring As String
    Dim enearray() As String
    Dim Elect As Boolean


    Public Sub RlZoneEVMain()

        'get the input and output files
        Call GetRlZEVFiles()

        'check if there is any value assigned to RlZEVSource - if not then set to constant as default
        If RlZEVSource = "" Then
            RlZEVSource = "Constant"
        End If

        'write header row to output file
        OutputRow = "Yeary,ZoneID,PopZy,GvaZy,Costy,Stationsy,CarFuely,NewTripsy,GJTy,ElPy"
        rlze.WriteLine(OutputRow)

        'if we are using a single scaling factor then set scaling factors - as a default they are just set to be constant over time
        If RlZPopSource = "Constant" Then
            PopGrowth = 1.005
        End If
        If RlZEcoSource = "Constant" Then
            GVAGrowth = 1.01
        End If
        If RlZEneSource = "Constant" Then
            CostGrowth = 1.01
            FuelGrowth = 1.01
        End If
        If RlZEVSource = "Constant" Then
            GJTGrowth = 1.0
            'note that proportion of electric trains is scaled using an additive factor rather than a multiplicative one
            ElPGrowth = 0.01
        End If

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        If NewRlZCap = True Then
            Call GetCapData()
        End If

        '1.3
        'get fuel efficiency values from the strategy file
        StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        stf = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
        'read header row
        stf.ReadLine()
        'v1.4 set fuel efficiency old to 1
        FuelEffOld(0) = 1
        FuelEffOld(1) = 1
        'v1.4 fuel efficiency change calculation corrected
        For y = 1 To 90
            'read line from file
            stratstring = stf.ReadLine()
            stratarray = Split(stratstring, ",")
            FuelEff(0, y) = stratarray(66) / FuelEffOld(0)
            FuelEff(1, y) = stratarray(67) / FuelEffOld(1)
            CO2Vol(0, y) = stratarray(74)
            CO2Vol(1, y) = stratarray(73)
            CO2Price(0, y) = stratarray(70)
            CO2Price(1, y) = stratarray(71)
            'also now get GJT growth value
            GJTProp(1, y) = stratarray(82)
            'v1.4 update FuelEffOld values
            FuelEffOld(0) = stratarray(66)
            FuelEffOld(1) = stratarray(67)
        Next
        stf.Close()

        'initiallize read elelct file
        Elect = True

        'read first line from electrification file
        Call ElectricRead()

        'loop through rows in input data file calculating the external variable files, until there are no rows left
        Call CalcRlZExtVars()

        'capdata

        rlzi.Close()
        rlze.Close()

    End Sub

    Sub GetRlZEVFiles()

        RlZoneInputData = New IO.FileStream(DirPath & "RailZoneInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rlzi = New IO.StreamReader(RlZoneInputData, System.Text.Encoding.Default)
        'read header row
        InputRow = rlzi.ReadLine

        RlZoneExtVar = New IO.FileStream(DirPath & EVFilePrefix & "RailZoneExtVar.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rlze = New IO.StreamWriter(RlZoneExtVar, System.Text.Encoding.Default)

        'if external variable values are based on an input file then get the external variable scaling file
        'If RlLEVSource = "File" Then
        '    RlZoneEVScale = New IO.FileStream(DirPath & EVFilePrefix & "RailZoneEVScaling.csv", IO.FileMode.Open, IO.FileAccess.Read)
        '    '***THIS SECTION IS COMMENTED OUT becuase it has been moved to the CalcRlZExtVars sub, because file only has 90 rows so have to reopen it for each file
        '    'rlzs = New IO.StreamReader(RlZoneEVScale, System.Text.Encoding.Default)
        '    'read header row
        '    'InputRow = rlzs.ReadLine
        'End If

        'if capacity is changing then get capacity change file
        If NewRlZCap = True Then
            RlZoneCapData = New IO.FileStream(DirPath & CapFilePrefix & "RailZoneCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
            rlzc = New IO.StreamReader(RlZoneCapData, System.Text.Encoding.Default)
            'read header row
            rlzc.ReadLine()
        End If

        RlZoneElSchemes = New IO.FileStream(DirPath & EVFilePrefix & "RailZoneElectrificationDates.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rzel = New IO.StreamReader(RlZoneElSchemes, System.Text.Encoding.Default)
        'read header row
        rzel.ReadLine()

        '**This has been moved here because need to reread the file for each zone
        'v1.6 just read at the start of each year
        If RlZOthSource = "File" Then
            RlZoneEVScale = New IO.FileStream(DirPath & "RailZoneEVScaling.csv", IO.FileMode.Open, IO.FileAccess.Read)
            rlzs = New IO.StreamReader(RlZoneEVScale, System.Text.Encoding.Default)
            'read header row
            InputRow = rlzs.ReadLine
        ElseIf RlZEneSource = "Database" Then
            'v1.3 altered so that scenario file is read directly as an input file
            ZoneEneFile = New IO.FileStream(DBaseEneFile, IO.FileMode.Open, IO.FileAccess.Read)
            zer = New IO.StreamReader(ZoneEneFile, System.Text.Encoding.Default)
            'read header row
            enestring = zer.ReadLine
        End If


    End Sub

    Sub CalcRlZExtVars()
        Dim ZoneID(238, 0) As Integer
        Dim Year As Integer
        Dim PopOld(238, 0) As Double
        Dim GVAOld(238, 0) As Double
        Dim CostOld(238, 0) As Double
        Dim StationsOld(238, 0) As Double
        Dim FuelOld(238, 0) As Double
        Dim GJTOld(238, 0) As Double
        Dim ElPOld(238, 0) As Double
        Dim PopNew As Double
        Dim GVANew As Double
        Dim CostNew As Double
        Dim FuelNew As Double
        Dim StationsNew(238, 0) As Double
        Dim NewTrips As Double
        Dim GJTNew As Double
        Dim ElPNew As Double
        Dim DieselOld(238, 0), DieselNew, ElectricOld(238, 0), ElectricNew As Double
        Dim DMaintOld(238, 0), EMaintOld(238, 0) As Double
        Dim ScalingRow As String
        Dim ScalingData() As String
        Dim Country(238, 0) As String
        Dim keylookup As String
        Dim newval As Double
        Dim InDieselOld(238, 0), InElectricOld(238, 0), InDieselNew, InElectricNew

        Dim diecarch, elecarch As Double
        Dim ElStat(238, 0) As Long
        Dim InputCount As Long
        Dim InDieselOldAll, InElectricOldAll, InDieselNewAll, InElectricNewAll

        Year = 1

        If RlZOthSource = "File" Then
            ScalingRow = rlzs.ReadLine
            ScalingData = Split(ScalingRow, ",")
        ElseIf RlZEneSource = "Database" Then
            enestring = zer.ReadLine
            enearray = Split(enestring, ",")
            InDieselOldAll = enearray(2)
            InElectricOldAll = enearray(3)
        End If


        Do Until Year > 90

            If RlZEneSource = "Database" Then
                enestring = zer.ReadLine
                enearray = Split(enestring, ",")
                InDieselNewAll = enearray(2)
                InElectricNewAll = enearray(3)
            End If
            InputCount = 1


            Do Until InputCount > 144

                If Year = 1 Then

                    InputRow = rlzi.ReadLine
                    InputData = Split(InputRow, ",")
                    ZoneID(InputCount, 0) = InputData(0)
                    PopOld(InputCount, 0) = InputData(4)
                    GVAOld(InputCount, 0) = InputData(5)
                    CostOld(InputCount, 0) = InputData(6)
                    StationsOld(InputCount, 0) = InputData(7)
                    FuelOld(InputCount, 0) = InputData(8)
                    GJTOld(InputCount, 0) = InputData(9)
                    Country(InputCount, 0) = InputData(10)
                    ElPOld(InputCount, 0) = InputData(11)
                    ElStat(InputCount, 0) = InputData(12)
                    NewTrips = 0

                    'need to set StationsNew to equal StationsOld to start with, as it gets reset every year but doesn't change every year
                    StationsNew(InputCount, 0) = StationsOld(InputCount, 0)

                    'if using scaling factors then need to set a base value for the diesel fuel cost for this zone
                    'can assume that 8.77% of total costs (which in all cases are set to 1) are made up of fuel, and that electric costs 55.3% of diesel price
                    '0.0877 = (ElP * DieselPrice * 0.553) + (DP * DieselPrice)
                    '0.0877 = DieselPrice((ElP * 0.553) + DP)
                    'DieselPrice = 0.0877/(0.553ElP + DP)
                    If RlZEneSource = "File" Then
                        DieselOld(InputCount, 0) = 0.0877 / ((0.553 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                        ElectricOld(InputCount, 0) = 0.553 * DieselOld(InputCount, 0)
                        'also need to set a base value for the maintenance and lease costs for this zone
                        'can assume that 26.62% of total costs (which in all cases are set to 1) are made up of maintenance and leasing, and that electric trains cost 75.8% of diesel trains
                        '0.2662 = (ElP * DMaint * 0.758) + (DP * DMaint)
                        '0.2662 = DMaint((ElP * 0.758) + DP)
                        'DMaint = 0.2662/(0.758ElP + DP)
                        DMaintOld(InputCount, 0) = 0.2662 / ((0.758 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                        EMaintOld(InputCount, 0) = 0.758 * DMaintOld(InputCount, 0)
                    ElseIf RlZEneSource = "Database" Then
                        InDieselOld(InputCount, 0) = InDieselOldAll
                        InElectricOld(InputCount, 0) = InElectricOldAll
                        DieselOld(InputCount, 0) = 29.204
                        ElectricOld(InputCount, 0) = 16.156
                        DMaintOld(InputCount, 0) = 37.282
                        EMaintOld(InputCount, 0) = 24.855
                    End If

                End If

                'if using scaling factors then read in the scaling factors for this year
                If RlZOthSource = "File" Then
                    'need to leave cost growth factor until we know the new proportion of electric/diesel trains
                    GJTGrowth = 1 + ScalingData(7)
                    ElPGrowth = ScalingData(8)
                Else
Elect:              'altered to allow reading in electrification input file***
                    If ZoneID(InputCount, 0) = ElectricZone Then
                        If Year = ElectricYear Then
                            ElPGrowth = 0.9 * (1 - ElPOld(InputCount, 0)) * (ElectricStations / (StationsOld(InputCount, 0) - ElStat(InputCount, 0)))
                            ElStat(InputCount, 0) += ElectricStations
                            Call ElectricRead()
                            If Elect = False Then
                                GoTo NextYear
                            End If
                        Else
                            ElPGrowth = 0
                        End If
                    Else
                        ElPGrowth = 0
                    End If

                    If ZoneID(InputCount, 0) = ElectricZone Then
                        If Year = ElectricYear Then
                            GoTo Elect
                        Else
                            GoTo NextYear
                        End If
                    End If
NextYear:
                End If

                If RlZPopSource = "Constant" Then
                    PopNew = PopOld(InputCount, 0) * PopGrowth
                End If
                If RlZPopSource = "File" Then
                    Select Case Country(InputCount, 0)
                        Case "E"
                            PopGrowth = 1 + ScalingData(1)
                        Case "S"
                            PopGrowth = 1 + ScalingData(2)
                        Case "W"
                            PopGrowth = 1 + ScalingData(3)
                    End Select
                    PopNew = PopOld(InputCount, 0) * PopGrowth
                End If

                If RlZPopSource = "Database" Then
                    'if year is after 2093 then no population forecasts are available so assume population remains constant
                    'now modified as population data available up to 2100 - so should never need 'else'
                    If Year < 91 Then
                        keylookup = Year & "_" & ZoneID(InputCount, 0)
                        If PopYearLookup.TryGetValue(keylookup, newval) Then
                            PopNew = newval
                        Else
                            ErrorString = "population found in lookup table for zone " & ZoneID(InputCount, 0) & " in year " & Year
                            Call DictionaryMissingVal()
                        End If
                    Else
                        PopNew = PopOld(InputCount, 0)
                    End If
                End If
                If RlZEcoSource = "Constant" Then
                    GVANew = GVAOld(InputCount, 0) * GVAGrowth
                ElseIf RlZEcoSource = "File" Then
                    GVAGrowth = 1 + ScalingData(4)
                    GVANew = GVAOld(InputCount, 0) * GVAGrowth
                ElseIf RlZEcoSource = "Database" Then
                    'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                    'now modified as gva data available up to 2100 - so should never need 'else'
                    If Year < 91 Then
                        keylookup = Year & "_" & ZoneID(InputCount, 0)
                        If EcoYearLookup.TryGetValue(keylookup, newval) Then
                            GVANew = newval
                        Else
                            ErrorString = "GVA found in lookup table for zone " & ZoneID(InputCount, 0) & " in year " & Year
                            Call DictionaryMissingVal()
                        End If
                    Else
                        GVANew = GVAOld(InputCount, 0)
                    End If
                End If

                ElPNew = ElPOld(InputCount, 0) + ElPGrowth
                'constrain proportion of electric trains to 1
                If ElPNew > 1 Then
                    ElPNew = 1
                End If

                'once we know new proportion of electric and diesel trains can calculate cost growth factor
                If RlZEneSource = "File" Then
                    'fuel forms 8.77% of costs, and in base year electric costs are set as being 0.553 times diesel costs - base prices set above
                    'scale both base prices
                    DieselNew = DieselOld(InputCount, 0) * (1 + ScalingData(5)) * FuelEff(1, Year)
                    ElectricNew = ElectricOld(InputCount, 0) * (1 + ScalingData(6)) * FuelEff(0, Year)
                    ''maintenance and leasing forms 26.62% of total costs, and in base year electric costs are set as being 0.758 times diesel costs - base prices set above
                    ''don't need to scale as assuming these costs remain constant per train over time
                    '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                    FuelGrowth = 1 + ScalingData(5)
                ElseIf RlZEneSource = "Database" Then
                    InDieselNew = InDieselNewAll
                    InElectricNew = InElectricNewAll

                    DieselNew = DieselOld(InputCount, 0) * (InDieselNew / InDieselOld(InputCount, 0)) * FuelEff(1, Year)
                    ElectricNew = ElectricOld(InputCount, 0) * (InElectricNew / InElectricOld(InputCount, 0)) * FuelEff(0, Year)
                    '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                    FuelGrowth = InDieselNew / InDieselOld(InputCount, 0)
                ElseIf RlZEneSource = "Constant" Then
                    DieselNew = DieselOld(InputCount, 0) * CostGrowth * FuelEff(1, Year)
                    ElectricNew = ElectricOld(InputCount, 0) * CostGrowth * FuelEff(0, Year)
                End If

                'v1.4 if carbon charge is applied then calculate it
                If RlCaCharge = True Then
                    'check if it is a relevant year
                    If Year >= CarbChargeYear Then
                        'calculation is: (base fuel units per km * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
                        'as a base assuming that diesel trains use 1.873 litres/train km and electric trains use 12.611 kWh/train km
                        diecarch = 1.873 * FuelEff(1, Year) * CO2Vol(1, Year) * (CO2Price(1, Year) / 10)
                        elecarch = 12.611 * FuelEff(0, Year) * CO2Vol(0, Year) * (CO2Price(0, Year) / 10)
                    Else
                        diecarch = 0
                        elecarch = 0
                    End If
                Else
                    diecarch = 0
                    elecarch = 0
                End If


                'multiply new prices by new proportions and add to fixed costs
                CostNew = 121.381 + ((DieselNew + diecarch) * (1 - ElPNew)) + ((ElectricNew + elecarch) * ElPNew) + (EMaintOld(InputCount, 0) * ElPNew) + (DMaintOld(InputCount, 0) * (1 - ElPNew))

                FuelNew = FuelOld(InputCount, 0) * FuelGrowth
                If RlZOthSource = "File" Then
                    GJTNew = GJTOld(InputCount, 0) * GJTGrowth
                Else
                    GJTNew = GJTOld(InputCount, 0) * GJTProp(1, Year)
                End If

                'if including capacity changes then check if there are any capacity changes on this flow
                If NewRlZCap = True Then
                    'if there are any capacity changes on this flow, check if there are any capacity changes in this year
                    If Year = CapYear Then
                        If ZoneID(InputCount, 0) = CapID Then
                            'if there are, then update the capacity variables, and read in the next row from the capacity file
                            StationsNew(InputCount, 0) = StationsOld(InputCount, 0) + StationChange
                            NewTrips = TripChange / StationChange
                            Call GetCapData()
                        End If
                    End If
                End If
                'write to output file
                OutputRow = Year & "," & ZoneID(InputCount, 0) & "," & PopNew & "," & GVANew & "," & CostNew & "," & StationsNew(InputCount, 0) & "," & FuelNew & "," & NewTrips & "," & GJTNew & "," & ElPNew
                rlze.WriteLine(OutputRow)

                'set old values as previous new values
                PopOld(InputCount, 0) = PopNew
                GVAOld(InputCount, 0) = GVANew
                CostOld(InputCount, 0) = CostNew
                FuelOld(InputCount, 0) = FuelNew
                StationsOld(InputCount, 0) = StationsNew(InputCount, 0)
                If RlZOthSource = "File" Then
                    GJTOld(InputCount, 0) = GJTNew
                End If
                ElPOld(InputCount, 0) = ElPNew
                DieselOld(InputCount, 0) = DieselNew
                ElectricOld(InputCount, 0) = ElectricNew
                If RlZEneSource = "Database" Then
                    InDieselOld(InputCount, 0) = InDieselNew
                    InElectricOld(InputCount, 0) = InElectricNew
                End If


                NewTrips = 0



                InputCount += 1
            Loop
            Year += 1
        Loop

        If RlZOthSource = "File" Then
            'close scaling data file
            rlzs.Close()
        End If

        If RlLEneSource = "Database" Then
            zer.Close()
        End If

    End Sub

    Sub GetCapData()
        InputRow = rlzc.ReadLine
        If InputRow Is Nothing Then
        Else
            InputData = Split(InputRow, ",")
            CapID = InputData(0)
            CapYear = InputData(1) - 2010
            StationChange = InputData(2)
            TripChange = InputData(3)
        End If
    End Sub

    Sub ElectricRead()
        Dim eleline As String
        Dim elearray() As String

        eleline = rzel.ReadLine
        If eleline Is Nothing Then
            ElectricZone = 0
            Elect = False
        Else
            elearray = Split(eleline, ",")
            ElectricZone = elearray(0)
            ElectricYear = elearray(1) - 2010
            ElectricStations = elearray(2)
        End If

    End Sub

    Sub DictionaryMissingVal()
        LogLine = "No " & ErrorString & " when updating input files.  Model run terminated."
        lf.WriteLine(LogLine)
        lf.Close()
        MsgBox("Model run failed.  Please consult the log file for details.")
        End
    End Sub
End Module
