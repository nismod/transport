Module RlZoneExtVarCalc1pt4
    '1.2 this version allows capacity changes to be included
    '1.2 it now allows external variable growth factors to be taken from an input file and to vary over time
    '1.3 this version allows input from the database
    'also includes fuel efficiency changes
    'now also includes electrification schemes
    '1.4 fuel efficiency calculation corrected

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

    Public Sub RlZoneEVMain()

        'get the input and output files
        Call GetRlZEVFiles()

        'check if there is any value assigned to RlZEVSource - if not then set to constant as default
        If RlZEVSource = "" Then
            RlZEVSource = "Constant"
        End If

        'write header row to output file
        OutputRow = "ZoneID,Yeary,PopZy,GvaZy,Costy,Stationsy,CarFuely,NewTripsy,GJTy,ElPy"
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

        'read first line from electrification file
        Call ElectricRead()

        'loop through rows in input data file calculating the external variable files, until there are no rows left
        Do
            InputRow = rlzi.ReadLine
            If InputRow Is Nothing Then
                Exit Do
            Else
                Call CalcRlZExtVars()
            End If
        Loop

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

    End Sub

    Sub CalcRlZExtVars()
        Dim ZoneID As Integer
        Dim Year As Integer
        Dim PopOld As Double
        Dim GVAOld As Double
        Dim CostOld As Double
        Dim StationsOld As Double
        Dim FuelOld As Double
        Dim GJTOld As Double
        Dim ElPOld As Double
        Dim PopNew As Double
        Dim GVANew As Double
        Dim CostNew As Double
        Dim FuelNew As Double
        Dim StationsNew As Double
        Dim NewTrips As Double
        Dim GJTNew As Double
        Dim ElPNew As Double
        Dim DieselOld, DieselNew, ElectricOld, ElectricNew As Double
        Dim DMaintOld, EMaintOld As Double
        Dim ScalingRow As String
        Dim ScalingData() As String
        Dim Country As String
        Dim keylookup As String
        Dim newval As Double
        Dim InDieselOld, InElectricOld, InDieselNew, InElectricNew
        Dim enestring As String
        Dim enearray() As String
        Dim diecarch, elecarch As Double
        Dim ElStat As Long

        InputData = Split(InputRow, ",")
        ZoneID = InputData(0)
        PopOld = InputData(4)
        GVAOld = InputData(5)
        CostOld = InputData(6)
        StationsOld = InputData(7)
        FuelOld = InputData(8)
        GJTOld = InputData(9)
        Country = InputData(10)
        ElPOld = InputData(11)
        ElStat = InputData(12)
        NewTrips = 0

        'need to set StationsNew to equal StationsOld to start with, as it gets reset every year but doesn't change every year
        StationsNew = StationsOld

        'if using scaling factors then need to set a base value for the diesel fuel cost for this zone
        'can assume that 8.77% of total costs (which in all cases are set to 1) are made up of fuel, and that electric costs 55.3% of diesel price
        '0.0877 = (ElP * DieselPrice * 0.553) + (DP * DieselPrice)
        '0.0877 = DieselPrice((ElP * 0.553) + DP)
        'DieselPrice = 0.0877/(0.553ElP + DP)
        If RlZEneSource = "File" Then
            DieselOld = 0.0877 / ((0.553 * ElPOld) + (1 - ElPOld))
            ElectricOld = 0.553 * DieselOld
            'also need to set a base value for the maintenance and lease costs for this zone
            'can assume that 26.62% of total costs (which in all cases are set to 1) are made up of maintenance and leasing, and that electric trains cost 75.8% of diesel trains
            '0.2662 = (ElP * DMaint * 0.758) + (DP * DMaint)
            '0.2662 = DMaint((ElP * 0.758) + DP)
            'DMaint = 0.2662/(0.758ElP + DP)
            DMaintOld = 0.2662 / ((0.758 * ElPOld) + (1 - ElPOld))
            EMaintOld = 0.758 * DMaintOld
        ElseIf RlZEneSource = "Database" Then
            'v1.3 altered so that scenario file is read directly as an input file
            ZoneEneFile = New IO.FileStream(DBaseEneFile, IO.FileMode.Open, IO.FileAccess.Read)
            zer = New IO.StreamReader(ZoneEneFile, System.Text.Encoding.Default)
            'read header row
            enestring = zer.ReadLine
            'read first line of data
            enestring = zer.ReadLine
            enearray = Split(enestring, ",")
            InDieselOld = enearray(2)
            InElectricOld = enearray(3)
            DieselOld = 29.204
            ElectricOld = 16.156
            DMaintOld = 37.282
            EMaintOld = 24.855
        End If

        'set year as 1 to start with
        Year = 1

        '**This has been moved here because need to reread the file for each zone
        If RlZOthSource = "File" Then
            RlZoneEVScale = New IO.FileStream(DirPath & "RailZoneEVScaling.csv", IO.FileMode.Open, IO.FileAccess.Read)
            rlzs = New IO.StreamReader(RlZoneEVScale, System.Text.Encoding.Default)
            'read header row
            InputRow = rlzs.ReadLine
        End If

        'loop through 90 years scaling up values and writing to the output file
        Do While Year < 91
            'if using scaling factors then read in the scaling factors for this year
            If RlZOthSource = "File" Then
                ScalingRow = rlzs.ReadLine
                ScalingData = Split(ScalingRow, ",")
                'need to leave cost growth factor until we know the new proportion of electric/diesel trains
                GJTGrowth = 1 + ScalingData(7)
                ElPGrowth = ScalingData(8)
            Else
                'altered to allow reading in electrification input file***
                If ZoneID = ElectricZone Then
                    If Year = ElectricYear Then
                        ElPGrowth = 0.9 * (1 - ElPOld) * (ElectricStations / (StationsOld - ElStat))
                        ElStat += ElectricStations
                        Call ElectricRead()
                    Else
                        ElPGrowth = 0
                    End If
                Else
                    ElPGrowth = 0
                End If
            End If

            If RlZPopSource = "Constant" Then
                PopNew = PopOld * PopGrowth
            End If
            If RlZPopSource = "File" Then
                Select Case Country
                    Case "E"
                        PopGrowth = 1 + ScalingData(1)
                    Case "S"
                        PopGrowth = 1 + ScalingData(2)
                    Case "W"
                        PopGrowth = 1 + ScalingData(3)
                End Select
                PopNew = PopOld * PopGrowth
            End If
            If RlZPopSource = "Database" Then
                'if year is after 2093 then no population forecasts are available so assume population remains constant
                'now modified as population data available up to 2100 - so should never need 'else'
                If Year < 91 Then
                    keylookup = Year & "_" & ZoneID
                    If PopYearLookup.TryGetValue(keylookup, newval) Then
                        PopNew = newval
                    Else
                        ErrorString = "population found in lookup table for zone " & ZoneID & " in year " & Year
                        Call DictionaryMissingVal()
                    End If
                Else
                    PopNew = PopOld
                End If
            End If
            If RlZEcoSource = "Constant" Then
                GVANew = GVAOld * GVAGrowth
            ElseIf RlZEcoSource = "File" Then
                GVAGrowth = 1 + ScalingData(4)
                GVANew = GVAOld * GVAGrowth
            ElseIf RlZEcoSource = "Database" Then
                'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                'now modified as gva data available up to 2100 - so should never need 'else'
                If Year < 91 Then
                    keylookup = Year & "_" & ZoneID
                    If EcoYearLookup.TryGetValue(keylookup, newval) Then
                        GVANew = newval
                    Else
                        ErrorString = "GVA found in lookup table for zone " & ZoneID & " in year " & Year
                        Call DictionaryMissingVal()
                    End If
                Else
                    GVANew = GVAOld
                End If
            End If

            ElPNew = ElPOld + ElPGrowth
            'constrain proportion of electric trains to 1
            If ElPNew > 1 Then
                ElPNew = 1
            End If
            'once we know new proportion of electric and diesel trains can calculate cost growth factor
            If RlZEneSource = "File" Then
                'fuel forms 8.77% of costs, and in base year electric costs are set as being 0.553 times diesel costs - base prices set above
                'scale both base prices
                DieselNew = DieselOld * (1 + ScalingData(5)) * FuelEff(1, Year)
                ElectricNew = ElectricOld * (1 + ScalingData(6)) * FuelEff(0, Year)
                ''maintenance and leasing forms 26.62% of total costs, and in base year electric costs are set as being 0.758 times diesel costs - base prices set above
                ''don't need to scale as assuming these costs remain constant per train over time
                '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                FuelGrowth = 1 + ScalingData(5)
            ElseIf RlZEneSource = "Database" Then
                enestring = zer.ReadLine
                enearray = Split(enestring, ",")
                InDieselNew = enearray(2)
                InElectricNew = enearray(3)
                DieselNew = DieselOld * (InDieselNew / InDieselOld) * FuelEff(1, Year)
                ElectricNew = ElectricOld * (InElectricNew / InElectricOld) * FuelEff(0, Year)
                '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                FuelGrowth = InDieselNew / InDieselOld
            ElseIf RlZEneSource = "Constant" Then
                DieselNew = DieselOld * CostGrowth * FuelEff(1, Year)
                ElectricNew = ElectricOld * CostGrowth * FuelEff(0, Year)
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
            CostNew = 121.381 + ((DieselNew + diecarch) * (1 - ElPNew)) + ((ElectricNew + elecarch) * ElPNew) + (EMaintOld * ElPNew) + (DMaintOld * (1 - ElPNew))

            FuelNew = FuelOld * FuelGrowth
            If RlZOthSource = "File" Then
                GJTNew = GJTOld * GJTGrowth
            Else
                GJTNew = GJTOld * GJTProp(1, Year)
            End If

            'if including capacity changes then check if there are any capacity changes on this flow
            If NewRlZCap = True Then
                If ZoneID = CapID Then
                    'if there are any capacity changes on this flow, check if there are any capacity changes in this year
                    If Year = CapYear Then
                        'if there are, then update the capacity variables, and read in the next row from the capacity file
                        StationsNew = StationsOld + StationChange
                        NewTrips = TripChange / StationChange
                        Call GetCapData()
                    End If
                End If
            End If
            'write to output file
            OutputRow = ZoneID & "," & Year & "," & PopNew & "," & GVANew & "," & CostNew & "," & StationsNew & "," & FuelNew & "," & NewTrips & "," & GJTNew & "," & ElPNew
            rlze.WriteLine(OutputRow)
            'set old values as previous new values
            PopOld = PopNew
            GVAOld = GVANew
            CostOld = CostNew
            FuelOld = FuelNew
            StationsOld = StationsNew
            If RlZOthSource = "File" Then
                GJTOld = GJTNew
            End If
            ElPOld = ElPNew
            DieselOld = DieselNew
            ElectricOld = ElectricNew
            If RlZEneSource = "Database" Then
                InDieselOld = InDieselNew
                InElectricOld = InElectricNew
            End If
            NewTrips = 0
            'update year
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
