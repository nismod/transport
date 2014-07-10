Module RlLinkExtVarCalc1pt5
    'creates an external variables file for the rail model, based on a single year's input data and growth factors for the external variables
    '1.2 this version allows capacity changes to be specified
    '1.2 it now allows external variable growth factors to be taken from an input file and to vary over time
    '1.3 this version allows input from the database
    '1.4 now makes use of a list of electrification schemes
    'and also takes account of changing fuel efficiencies
    'and also gives the option of a carbon-based charge
    'now also includes rail zone electrification
    'v1.6 now calculate by annual timesteps
    'and InDieselOldAll, InElectricOldAll, InDieselNewAll, InElectricNewAll are created to replace the old parameters
    'and old InDieselOld/New and InElectricOld/New now increase dimension to seperate the values for each link
    'and each time the calculation start, the values in new parameters (InDieselOldAll etc) will be read into the original parameters

    Dim RlLinkInputData As IO.FileStream
    Dim rai As IO.StreamReader
    Dim RlLinkExtVar As IO.FileStream
    Dim rae As IO.StreamWriter
    Dim RlLinkCapData As IO.FileStream
    Dim rac As IO.StreamReader
    Dim RlLinkEVScale As IO.FileStream
    Dim ras As IO.StreamReader
    Dim RlLinkElSchemes As IO.FileStream
    Dim res As IO.StreamWriter
    Dim rel As IO.StreamReader
    Dim RlLinkNewCapData As IO.FileStream
    Dim rlnc As IO.StreamWriter
    Dim rlnr As IO.StreamReader
    Dim InputCount As Integer
    Dim InputRow As String
    Dim InputData() As String
    Dim OutputRow As String
    Dim OPopGrowth, DPopGrowth As Double
    Dim OGVAGrowth, DGVAGrowth As Double
    Dim CostGrowth As Double
    Dim FuelGrowth As Double
    Dim MaxTDGrowth As Double
    Dim ElPGrowth As Double
    Dim CapID As Long
    Dim CapYear, CapNewYear As Integer
    Dim TrackChange As Integer
    Dim MaxTDChange As Double
    Dim ErrorString As String
    Dim ElectFlow, ElectYear, ElectTracks As Long
    Dim stf As IO.StreamReader
    Dim stratstring As String
    Dim FuelEff(1, 90), CO2Vol(1, 90), CO2Price(1, 90), MaxTD(90) As Double
    Dim OutString As String
    Dim CapCount As Double
    Dim AddingCap As Boolean
    Dim TracksToBuild, CapLanes As Double
    Dim CapType, CapRow As String
    Dim TrainChange As Double
    Dim NewCapDetails(455, 4) As Double
    Dim Breakout As Boolean
    Dim sortarray(11) As String
    Dim sortedline As String
    Dim splitline() As String
    Dim arraynum As Long
    Dim padflow, padyear As String
    Dim NewTrains As Double
    Dim FuelEffOld(2) As Double
    Dim Elect As Boolean
    Dim CapArray(455, 5) As String
    Dim NewCapArray(238, 4) As String
    Dim CapNum As Integer
    Dim InputArray(238, 17) As String
    Dim OutputArray(238, 11) As String
    Dim stratarray(90, 95) As String
    Dim elearray(238, 3) As String
    Dim EleNum As Integer
    Dim RlElNum As Integer
    Dim RzElNum As Integer







    Public Sub RailLinkEVMain()

        'get the input and output file names
        Call GetFiles()

        'check if there is any value assigned to RlLEVSource - if not then set to constant as default
        If RlLEVSource = "" Then
            RlLEVSource = "Constant"
        End If

        'if we are using a single scaling factor then set scaling factors - as a default they are just set to be constant over time
        If RlLPopSource = "Constant" Then
            OPopGrowth = 1.005
            DPopGrowth = 1.005
        End If
        If RlLEcoSource = "Constant" Then
            OGVAGrowth = 1.01
            DGVAGrowth = 1.01
        End If
        If RlLEneSource = "Constant" Then
            CostGrowth = 1.01
            FuelGrowth = 1.01
        End If
        If RlLEVSource = "Constant" Then
            MaxTDGrowth = 1
            'note that proportion of electric trains is scaled using an additive factor rather than a multiplicative one
            ElPGrowth = 0.025
        End If

        'read initial input data
        Call ReadData("RailLink", "Input", InputArray, True)

        Call ReadData("RailLink", "CapChange", CapArray)
        CapNum = 1
        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.4 change - now read this anyway to deal with compulsory enhancements
        'so we created another file containing sorted implemented capacity enhancements (in get files sub)
        'need initial file to be sorted by scheme type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array
        CapCount = 0
        AddingCap = False
        TracksToBuild = 0
        Do Until CapArray(CapNum, 0) Is Nothing
            Call GetCapData()
            Select Case CapType
                Case "C"
                    NewCapDetails(CapCount, 0) = CapID
                    NewCapDetails(CapCount, 1) = CapYear
                    NewCapDetails(CapCount, 2) = TrackChange
                    NewCapDetails(CapCount, 3) = MaxTDChange
                    NewCapDetails(CapCount, 4) = TrainChange
                    CapNewYear = CapYear
                Case "O"
                    'then if adding optional capacity read all optional dated enhancements to intermediate array
                    If NewRlLCap = True Then
                        If CapYear >= 0 Then
                            NewCapDetails(CapCount, 0) = CapID
                            NewCapDetails(CapCount, 1) = CapYear
                            NewCapDetails(CapCount, 2) = TrackChange
                            NewCapDetails(CapCount, 3) = MaxTDChange
                            NewCapDetails(CapCount, 4) = TrainChange
                            CapNewYear = CapYear
                        Else
                            'finally add all other enhancements to intermediate array until we have run out of additional capacity
                            If TracksToBuild >= TrackChange Then
                                NewCapDetails(CapCount, 0) = CapID
                                NewCapDetails(CapCount, 1) = CapNewYear
                                NewCapDetails(CapCount, 2) = TrackChange
                                NewCapDetails(CapCount, 3) = MaxTDChange
                                NewCapDetails(CapCount, 4) = TrainChange
                                TracksToBuild = TracksToBuild - TrackChange
                            Else
                                Do Until TracksToBuild >= TrackChange
                                    CapNewYear += 1
                                    If CapNewYear > 90 Then
                                        Breakout = True
                                        Exit Select
                                    End If
                                    TracksToBuild += NewRailTracks
                                Loop
                                NewCapDetails(CapCount, 0) = CapID
                                NewCapDetails(CapCount, 1) = CapNewYear
                                NewCapDetails(CapCount, 2) = TrackChange
                                NewCapDetails(CapCount, 3) = MaxTDChange
                                NewCapDetails(CapCount, 4) = TrainChange
                                TracksToBuild = TracksToBuild - TrackChange
                            End If
                        End If
                    Else
                        Exit Do
                    End If
            End Select
            If Breakout = True Then
                Exit Do
            End If
            CapCount += 1
        Loop
        'then sort the intermediate array by flow ID, then by year of implementation
        ReDim sortarray(CapCount - 1)
        For v = 0 To (CapCount - 1)
            padflow = String.Format("{0:000}", NewCapDetails(v, 0))
            padyear = String.Format("{0:00}", NewCapDetails(v, 1))
            sortarray(v) = padyear & "&" & padflow & "&" & v
        Next
        Array.Sort(sortarray)
        'write all lines to intermediate capacity file
        For v = 0 To (CapCount - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            NewCapArray(v + 1, 0) = NewCapDetails(arraynum, 0)
            NewCapArray(v + 1, 1) = NewCapDetails(arraynum, 1)
            NewCapArray(v + 1, 2) = NewCapDetails(arraynum, 2)
            NewCapArray(v + 1, 3) = NewCapDetails(arraynum, 3)
            NewCapArray(v + 1, 4) = NewCapDetails(arraynum, 4)
        Next

        Call WriteData("RailLink", "NewCap", NewCapArray)

        'reopen the capacity file as a reader
        CapNum = 1
        ReDim CapArray(238, 4)
        AddingCap = True
        Call GetCapData()

        'If NewRlLCap = True Then
        '    Call GetCapData()
        'End If

        'if including rail electrification then create the intermediate file sorted by flow then by date
        'mod - now do this anyway as some schemes are non-discretionary
        'create intermediate file listing timings of scheme implementations
        Call CreateElectrificationList()
        'initiallize read elelct file
        Elect = True
        'read the electrification list file as an input file
        'read ele scheme info
        Call ReadData("RailLink", "ElSchemes", elearray)
        EleNum = 1
        Call GetElectData()

        'v1.4
        'get fuel efficiency and other values from the strategy file
        Call ReadData("Strategy", "", stratarray)
        'v1.5 set fuel efficiency old to 1
        FuelEffOld(0) = 1
        FuelEffOld(1) = 1
        'v1.5 fuel efficiency change calculation corrected
        For y = 1 To 90
            'read line from file
            FuelEff(0, y) = stratarray(y, 66) / FuelEffOld(0)
            FuelEff(1, y) = stratarray(y, 67) / FuelEffOld(1)
            CO2Vol(0, y) = stratarray(y, 74)
            CO2Vol(1, y) = stratarray(y, 73)
            CO2Price(0, y) = stratarray(y, 70)
            CO2Price(1, y) = stratarray(y, 71)
            MaxTD(y) = stratarray(y, 78)
            'v1.5 update FuelEffOld values
            FuelEffOld(0) = stratarray(y, 66)
            FuelEffOld(1) = stratarray(y, 67)
        Next

        'loop through rows in input data file calculating the external variable values
        Call CalcExtVars()


    End Sub

    Sub GetFiles()

        'RlLinkInputData = New IO.FileStream(DirPath & "RailLinkInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        'rai = New IO.StreamReader(RlLinkInputData, System.Text.Encoding.Default)
        ''read header row
        'InputRow = rai.ReadLine

        'RlLinkExtVar = New IO.FileStream(DirPath & EVFilePrefix & "RailLinkExtVar.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        'rae = New IO.StreamWriter(RlLinkExtVar, System.Text.Encoding.Default)
        ''write header row to output file
        'OutputRow = "Yeary,FlowID,Tracksy,PopZ1y,PopZ2y,GVAZ1y,GVAZ2y,Costy,CarFuely,MaxTDy,ElPy,ElTracksy,AddTrainsy"
        'rae.WriteLine(OutputRow)

        ''if capacity is changing then get capacity change file
        ''v1.4 do this anyway to include compulsory changes
        'RlLinkCapData = New IO.FileStream(DirPath & CapFilePrefix & "RailLinkCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        'rac = New IO.StreamReader(RlLinkCapData, System.Text.Encoding.Default)
        ''read header row
        'rac.ReadLine()
        ''v1.4 new intermediate capacity file
        'RlLinkNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "RailLinkNewCap.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        'rlnc = New IO.StreamWriter(RlLinkNewCapData, System.Text.Encoding.Default)
        ''write header row
        'OutString = "FlowID,ChangeYear,TrackChange,MaxTDChange,TrainChange"
        'rlnc.WriteLine(OutString)


        'If NewRlLCap = True Then
        '    RlLinkCapData = New IO.FileStream(DirPath & CapFilePrefix & "RailLinkCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        '    rac = New IO.StreamReader(RlLinkCapData, System.Text.Encoding.Default)
        '    'read header row
        '    rac.ReadLine()
        'End If


    End Sub

    Sub CalcExtVars()

        Dim FlowID(238, 0) As Integer
        Dim Year As Integer
        Dim Tracks(238, 0) As Integer
        Dim Pop1Old(238, 0) As Double
        Dim Pop2Old(238, 0) As Double
        Dim GVA1Old(238, 0) As Double
        Dim GVA2Old(238, 0) As Double
        Dim CostOld(238, 0) As Double
        Dim FuelOld(238, 0) As Double
        Dim Pop1New As Double
        Dim Pop2New As Double
        Dim GVA1New As Double
        Dim GVA2New As Double
        Dim CostNew As Double
        Dim FuelNew As Double
        Dim MaxTDOld(238, 0), MaxTDNew As Double
        Dim DieselOld(238, 0), DieselNew, ElectricOld(238, 0), ElectricNew As Double
        Dim DMaintOld(238, 0), EMaintOld(238, 0) As Double
        Dim ElPOld(238, 0), ElPNew As Double
        Dim ScalingData(90, 8) As String
        Dim OCountry(238, 0), DCountry(238, 0) As String
        Dim OZone(238, 0), DZone(238, 0) As Long
        Dim keylookup As String
        Dim newval As Double
        Dim ElectTracksOld(238, 0), ElectTracksNew As Double
        Dim InDieselOld(238, 0), InElectricOld(238, 0), InDieselNew, InElectricNew As Double
        Dim enearray(91, 6) As String
        Dim diecarch, elecarch As Double
        Dim InDieselOldAll, InElectricOldAll, InDieselNewAll, InElectricNewAll


        'need to set a base value for the diesel fuel cost for this zone
        If RlLEneSource = "Database" Then
            Call ReadData("Energy", "", enearray)
            InDieselOldAll = enearray(1, 2)
            InElectricOldAll = enearray(1, 3)
        End If

        'get scaling factor file if we are using one
        If RlLOthSource = "File" Then
            Call ReadData("RailLink", "EVScale", ScalingData)
        End If

        'set year as 1 to start with
        Year = 1

        Do While Year < 91
            'loop through scaling up values for each year and writing to output file

            If RlLEneSource = "Database" Then

                InDieselNewAll = enearray(Year + 1, 2)
                InElectricNewAll = enearray(Year + 1, 3)

            End If

            If RlLOthSource = "File" Then
                'if using scaling factors then read in the scaling factors for this year
            End If

            InputCount = 1

            Do Until InputCount > 238
                If Year = 1 Then
                    FlowID(InputCount, 0) = InputArray(InputCount, 0)
                    OZone(InputCount, 0) = InputArray(InputCount, 1)
                    DZone(InputCount, 0) = InputArray(InputCount, 2)
                    Tracks(InputCount, 0) = InputArray(InputCount, 3)
                    Pop1Old(InputCount, 0) = InputArray(InputCount, 5)
                    Pop2Old(InputCount, 0) = InputArray(InputCount, 6)
                    GVA1Old(InputCount, 0) = InputArray(InputCount, 7)
                    GVA2Old(InputCount, 0) = InputArray(InputCount, 8)
                    CostOld(InputCount, 0) = InputArray(InputCount, 10)
                    FuelOld(InputCount, 0) = InputArray(InputCount, 11)
                    MaxTDOld(InputCount, 0) = InputArray(InputCount, 12)
                    ElPOld(InputCount, 0) = InputArray(InputCount, 13)
                    OCountry(InputCount, 0) = InputArray(InputCount, 14)
                    DCountry(InputCount, 0) = InputArray(InputCount, 15)
                    ElectTracksOld(InputCount, 0) = InputArray(InputCount, 16)
                    NewTrains = 0

                    If RlLEneSource = "Database" Then
                        InDieselOld(InputCount, 0) = InDieselOldAll
                        InElectricOld(InputCount, 0) = InElectricOldAll
                        DieselOld(InputCount, 0) = 29.204
                        ElectricOld(InputCount, 0) = 16.156
                        DMaintOld(InputCount, 0) = 37.282
                        EMaintOld(InputCount, 0) = 24.855
                    Else
                        'can assume that 8.77% of total costs (which in all cases are set to 1) are made up of fuel, and that electric costs 55.3% of diesel price
                        '0.0877 = (ElP * DieselPrice * 0.553) + (DP * DieselPrice)
                        '0.0877 = DieselPrice((ElP * 0.553) + DP)
                        'DieselPrice = 0.0877/(0.553ElP + DP)
                        DieselOld(InputCount, 0) = 0.0877 / ((0.553 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                        ElectricOld(InputCount, 0) = 0.553 * DieselOld(InputCount, 0)
                        'also need to set a base value for the maintenance and lease costs for this zone
                        'can assume that 26.62% of total costs (which in all cases are set to 1) are made up of maintenance and leasing, and that electric trains cost 75.8% of diesel trains
                        '0.2662 = (ElP * DMaint * 0.758) + (DP * DMaint)
                        '0.2662 = DMaint((ElP * 0.758) + DP)
                        'DMaint = 0.2662/(0.758ElP + DP)
                        DMaintOld(InputCount, 0) = 0.2662 / ((0.758 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                        EMaintOld(InputCount, 0) = 0.758 * DMaintOld(InputCount, 0)

                    End If



                End If

                If RlLPopSource = "Constant" Then
                    Pop1New = Pop1Old(InputCount, 0) * OPopGrowth
                    Pop2New = Pop2Old(InputCount, 0) * DPopGrowth
                ElseIf RlLPopSource = "File" Then
                    Select Case OCountry(InputCount, 0)
                        Case "E"
                            OPopGrowth = 1 + ScalingData(Year, 1)
                        Case "S"
                            OPopGrowth = 1 + ScalingData(Year, 2)
                        Case "W"
                            OPopGrowth = 1 + ScalingData(Year, 3)
                    End Select
                    Select Case DCountry(InputCount, 0)
                        Case "E"
                            DPopGrowth = 1 + ScalingData(Year, 1)
                        Case "S"
                            DPopGrowth = 1 + ScalingData(Year, 2)
                        Case "W"
                            DPopGrowth = 1 + ScalingData(Year, 3)
                    End Select
                    Pop1New = Pop1Old(InputCount, 0) * OPopGrowth
                    Pop2New = Pop2Old(InputCount, 0) * DPopGrowth
                ElseIf RlLPopSource = "Database" Then
                    'if year is after 2093 then no population forecasts are available so assume population remains constant
                    'now modified as population data available up to 2100 - so should never need 'else'
                    If Year < 91 Then
                        keylookup = Year & "_" & OZone(InputCount, 0)
                        If PopYearLookup.TryGetValue(keylookup, newval) Then
                            Pop1New = newval
                        Else
                            ErrorString = "population found in lookup table for zone " & OZone(InputCount, 0) & " in year " & Year
                            Call DictionaryMissingVal()
                        End If
                        keylookup = Year & "_" & DZone(InputCount, 0)
                        If PopYearLookup.TryGetValue(keylookup, newval) Then
                            Pop2New = newval
                        Else
                            ErrorString = "population found in lookup table for zone " & DZone(InputCount, 0) & " in year " & Year
                            Call DictionaryMissingVal()
                        End If
                    Else
                        Pop1New = Pop1Old(InputCount, 0)
                        Pop2New = Pop2Old(InputCount, 0)
                    End If
                End If
                If RlLEcoSource = "Constant" Then
                    GVA1New = GVA1Old(InputCount, 0) * OGVAGrowth
                    GVA2New = GVA2Old(InputCount, 0) * DGVAGrowth
                ElseIf RlLEcoSource = "File" Then
                    OGVAGrowth = 1 + ScalingData(Year, 4)
                    DGVAGrowth = 1 + ScalingData(Year, 4)
                    GVA1New = GVA1Old(InputCount, 0) * OGVAGrowth
                    GVA2New = GVA2Old(InputCount, 0) * DGVAGrowth
                ElseIf RlLEcoSource = "Database" Then
                    'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                    'now modified as GVA data available up to 2100 - so should never need 'else'
                    If Year < 91 Then
                        keylookup = Year & "_" & OZone(InputCount, 0)
                        If EcoYearLookup.TryGetValue(keylookup, newval) Then
                            GVA1New = newval
                        Else
                            ErrorString = "GVA found in lookup table for zone " & OZone(InputCount, 0) & " in year " & Year
                            Call DictionaryMissingVal()
                        End If
                        keylookup = Year & "_" & DZone(InputCount, 0)
                        If EcoYearLookup.TryGetValue(keylookup, newval) Then
                            GVA2New = newval
                        Else
                            ErrorString = "GVA found in lookup table for zone " & DZone(InputCount, 0) & " in year " & Year
                            Call DictionaryMissingVal()
                        End If
                    Else
                        GVA1New = GVA1Old(InputCount, 0)
                        GVA2New = GVA2Old(InputCount, 0)
                    End If
                End If
                'need to leave cost growth factor until we know new proportion of electric/diesel trains
                If RlLOthSource = "File" Then
                    'MaxTDGrowth = 1 + ScalingData(7)
                    ElPGrowth = ScalingData(Year, 8)
                End If

                'check if using list of electrification schemes
                ''mod - now do this anyway as some schemes are non-discretionary
                'If RlElect = True Then
                'check if in correct year for the current scheme
Elect:
                If Year = ElectYear Then
                    'if so check if correct row for the current scheme
                    If FlowID(InputCount, 0) = ElectFlow Then
                        'if so, then need to alter proportions of diesel and electric trains
                        ElPNew = ElPOld(InputCount, 0) + (0.9 * ((1 - ElPOld(InputCount, 0)) * (ElectTracks / (Tracks(InputCount, 0) - ElectTracksOld(InputCount, 0)))))
                        ElectTracksNew = ElectTracksOld(InputCount, 0) + ElectTracks
                        'read next scheme from list
                        Call GetElectData()

                        'if there is no data left in the Elect file then go to next calculation
                        If Elect = False Then
                            GoTo NextYear
                        End If

                    Else
                        ElPNew = ElPOld(InputCount, 0)
                        ElectTracksNew = ElectTracksOld(InputCount, 0)
                    End If
                Else
                    ElPNew = ElPOld(InputCount, 0)
                    ElectTracksNew = ElectTracksOld(InputCount, 0)
                End If

                If FlowID(InputCount, 0) = ElectFlow Then
                    If Year = ElectYear Then
                        GoTo Elect
                    Else
                        GoTo NextYear
                    End If
                End If
NextYear:
                'Else
                '    '***1.4 commented out, as don't want any growth if not using list of schemes
                '    'ElPNew = ElPOld + ElPGrowth#
                '    ElPNew = ElPOld
                '    ElectTracksNew = ElectTracksOld
                'End If
                'constrain proportion of electric trains to 1
                If ElPNew > 1 Then
                    ElPNew = 1
                End If
                'once we know new proportion of electric and diesel trains can calculate cost growth factor
                If RlLEneSource = "File" Then
                    'fuel forms 8.77% of costs, and in base year electric costs are set as being 0.553 times diesel costs - base prices set above
                    'scale both base prices
                    DieselNew = DieselOld(InputCount, 0) * (1 + ScalingData(Year, 5)) * FuelEff(1, Year)
                    ElectricNew = ElectricOld(InputCount, 0) * (1 + ScalingData(Year, 6)) * FuelEff(0, Year)
                    '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                    FuelGrowth = 1 + ScalingData(Year, 5)
                ElseIf RlLEneSource = "Constant" Then
                    DieselNew = DieselOld(InputCount, 0) * CostGrowth * FuelEff(1, Year)
                    ElectricNew = ElectricOld(InputCount, 0) * CostGrowth * FuelEff(0, Year)
                ElseIf RlLEneSource = "Database" Then
                    InDieselNew = InDieselNewAll
                    InElectricNew = InElectricNewAll
                    DieselNew = DieselOld(InputCount, 0) * (InDieselNew / InDieselOld(InputCount, 0)) * FuelEff(1, Year)
                    ElectricNew = ElectricOld(InputCount, 0) * (InElectricNew / InElectricOld(InputCount, 0)) * FuelEff(0, Year)
                    '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                    FuelGrowth = InDieselNew / InDieselOld(InputCount, 0)
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

                'maintenance and leasing forms 26.62% of total costs, and in base year electric costs are set as being 0.758 times diesel costs - base prices set above
                'don't need to scale as assuming these costs remain constant per train over time
                'multiply new prices by new proportions and add to fixed costs
                'v1.4 replaced old fixed costs of 0.6461 with fixed cost of 121.381p
                CostNew = 121.381 + ((DieselNew + diecarch) * (1 - ElPNew)) + ((ElectricNew + elecarch) * ElPNew) + (EMaintOld(InputCount, 0) * ElPNew) + (DMaintOld(InputCount, 0) * (1 - ElPNew))
                'estimate new fuel efficiency for road vehicles
                FuelNew = FuelOld(InputCount, 0) * FuelGrowth
                'if including capacity changes, then check if there are any capacity changes on this flow
                'v1.4 changed to include compulsory capacity changes where construction has already begun
                'all this involves is removing the if newrllcap = true clause, because this was already accounted for when generating the intermediate file, and adding a lineread above getcapdata because this sub was amended

                'if there are any capacity changes on this flow, check if there are any capacity changes in this year
                If Year = CapYear Then
                    If FlowID(InputCount, 0) = CapID Then
                        'if there are, then update the capacity variables, and read in the next row from the capacity file
                        Tracks(InputCount, 0) += TrackChange
                        'note that MaxTDChange now doesn't work - replaced by strategy common variables file
                        MaxTDOld(InputCount, 0) += MaxTDChange
                        NewTrains = TrainChange
                        Call GetCapData()
                    End If
                End If

                MaxTDNew = MaxTD(Year)
                'write to output file
                OutputArray(InputCount, 0) = Year
                OutputArray(InputCount, 1) = FlowID(InputCount, 0)
                OutputArray(InputCount, 2) = Tracks(InputCount, 0)
                OutputArray(InputCount, 3) = Pop1New & "," & Pop2New
                OutputArray(InputCount, 4) = GVA1New
                OutputArray(InputCount, 5) = GVA2New
                OutputArray(InputCount, 6) = CostNew
                OutputArray(InputCount, 7) = FuelNew
                OutputArray(InputCount, 8) = MaxTDNew
                OutputArray(InputCount, 9) = ElPNew
                OutputArray(InputCount, 10) = ElectTracksNew
                OutputArray(InputCount, 11) = NewTrains

                'set old values as previous new values
                Pop1Old(InputCount, 0) = Pop1New
                Pop2Old(InputCount, 0) = Pop2New
                GVA1Old(InputCount, 0) = GVA1New
                GVA2Old(InputCount, 0) = GVA2New
                CostOld(InputCount, 0) = CostNew
                FuelOld(InputCount, 0) = FuelNew
                MaxTDOld(InputCount, 0) = MaxTDNew
                ElPOld(InputCount, 0) = ElPNew
                ElectTracksOld(InputCount, 0) = ElectTracksNew
                DieselOld(InputCount, 0) = DieselNew
                ElectricOld(InputCount, 0) = ElectricNew
                If RlLEneSource = "Database" Then
                    InDieselOld(InputCount, 0) = InDieselNew
                    InElectricOld(InputCount, 0) = InElectricNew
                End If
                NewTrains = 0
                'update year
                InputCount += 1
            Loop

            'create file if year 1, otherwise update
            If Year = 1 Then
                Call WriteData("RailLink", "ExtVar", OutputArray, , True)
            Else
                Call WriteData("RailLink", "ExtVar", OutputArray, , False)
            End If

            Year += 1

            Loop



    End Sub

    Sub GetCapData()
        'modified in v1.8
        If AddingCap = True Then
            For i = 0 To UBound(CapArray, 2)
                CapArray(CapNum, i) = NewCapArray(CapNum, i)
            Next
        End If

        If CapArray(CapNum, 0) Is Nothing Then
        Else
            CapID = CapArray(CapNum, 0)
            If CapArray(CapNum, 1) = "-1" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = CapArray(CapNum, 1) - 2010
                Else
                    CapYear = CapArray(CapNum, 1)
                End If
            End If
            TrackChange = CapArray(CapNum, 2)
            MaxTDChange = CapArray(CapNum, 3)
            TrainChange = CapArray(CapNum, 4)
            If AddingCap = False Then
                CapType = CapArray(CapNum, 5)
            End If
            CapNum += 1
        End If

        'InputRow = rac.ReadLine
        'If InputRow Is Nothing Then
        'Else
        '    InputData = Split(InputRow, ",")
        '    CapID = InputData(0)
        '    CapYear = InputData(1) - 2010
        '    TrackChange = InputData(2)
        '    MaxTDChange = InputData(3)
        'End If
    End Sub

    Sub DictionaryMissingVal()
        LogLine = "No " & ErrorString & " when updating rail link external variable file.  Model run terminated."
        lf.WriteLine(LogLine)
        lf.Close()
        MsgBox("Model run failed.  Please consult the log file for details.")
        End
    End Sub

    Sub CreateElectrificationList()
        'now modified to include some schemes as standard
        'now modified to include zones as well as links

        Dim SchemeListFile As IO.FileStream
        Dim slf As IO.StreamReader
        Dim schemeinputrow As String
        Dim schemeoutputrow(28, 3) As String
        Dim elschemes(244, 3) As Double
        Dim schemearray(245, 5) As String
        Dim rownum As Integer
        Dim elyear As Long
        Dim eltrackkm As Double
        Dim kmtoelectrify As Double
        Dim sortarray(244) As String
        Dim sortedline As String
        Dim splitline() As String
        Dim arraynum, schemecount As Long
        Dim schemetype As String
        Dim ZoneListFile As IO.FileStream
        Dim zlf As IO.StreamReader
        Dim RlZoneELSchemes As IO.FileStream
        Dim rze As IO.StreamWriter
        Dim zoneinputrow As String
        Dim zoneoutputrow(33, 2) As String
        Dim zonearray(336, 4) As String
        Dim schemecode As String
        Dim elzschemes(335, 2) As Long
        Dim znum As Integer
        Dim zonecheck As Boolean

        'read old link scheme file
        Call ReadData("RailLink", "OldRlEl", schemearray)

        'read old zone scheme file
        Call ReadData("RailLink", "OldRzEl", zonearray)

        kmtoelectrify = 0

        'initialize the Railink and Railzone electrification array
        RlElNum = 1
        RzElNum = 1

        rownum = 0
        schemecount = -1
        'read details of first zone scheme
        znum = 0
        zonecheck = True
        'loop through all rows in the initial file assigning a year if schemes don't yet have one and writing values to array
        Do Until schemearray(RlElNum, 0) Is Nothing
            'check the scheme type
            schemetype = schemearray(RlElNum, 5)
            If schemetype = "C" Then
                'if it is a compulsory scheme then load values into array
                For v = 0 To 3
                    elschemes(rownum, v) = schemearray(RlElNum, v)
                Next
                elyear = schemearray(RlElNum, 1)
                schemecount += 1
                'now check for relevant zone changes
                schemecode = schemearray(RlElNum, 4)
                Do While zonecheck = True
                    If schemecode = zonearray(RzElNum, 3) Then
                        elzschemes(znum, 0) = zonearray(RzElNum, 0)
                        elzschemes(znum, 1) = zonearray(RzElNum, 1)
                        elzschemes(znum, 2) = zonearray(RzElNum, 2)
                        RzElNum += 1
                        If zonearray(RzElNum, 0) Is Nothing Then
                            zonecheck = False
                        Else
                            znum += 1
                            zonecheck = True
                        End If
                    Else
                        zonecheck = False
                    End If
                Loop
            Else
                'check if we are using optional electrification schemes
                If RlElect = True Then
                    'check if a year is already assigned
                    If schemearray(RlElNum, 1) = "" Then
                        'if it isn't then first get the length of track km for the scheme 
                        eltrackkm = schemearray(RlElNum, 2) * schemearray(RlElNum, 3)
                        'then check if there are any spare electrification km in the pot
                        If kmtoelectrify >= eltrackkm Then
                            'if there are enough, then assign this scheme to this year and load values into array
                            elschemes(rownum, 0) = schemearray(RlElNum, 0)
                            elschemes(rownum, 1) = elyear
                            elschemes(rownum, 2) = schemearray(RlElNum, 2)
                            elschemes(rownum, 3) = schemearray(RlElNum, 3)
                            'subtract the electrified km from the spare km
                            kmtoelectrify = kmtoelectrify - eltrackkm
                            schemecount += 1
                            schemecode = schemearray(RlElNum, 4)
                            Do While zonecheck = True
                                If schemecode = zonearray(RzElNum, 3) Then
                                    elzschemes(znum, 0) = zonearray(RzElNum, 0)
                                    elzschemes(znum, 1) = elyear
                                    elzschemes(znum, 2) = zonearray(RzElNum, 2)
                                    RzElNum += 1
                                    If zonearray(RzElNum, 0) Is Nothing Then
                                        zonecheck = False
                                    Else
                                        znum += 1
                                        zonecheck = True
                                    End If
                                Else
                                    zonecheck = False
                                End If
                            Loop
                        Else
                            'if there aren't, then move on to next year and add in a further allocation of track km
                            'loop until there are enough km in the pot to electrify the scheme
                            Do Until kmtoelectrify >= eltrackkm
                                elyear += 1
                                If elyear > 2100 Then
                                    Exit Do
                                End If
                                kmtoelectrify += ElectKmPerYear
                            Loop
                            'check if enough track km - if there aren't then it means we have reached 2100 so exit do loop
                            If kmtoelectrify >= eltrackkm Then
                                'if there are enough, then assign this scheme to this year and load values into array
                                elschemes(rownum, 0) = schemearray(RlElNum, 0)
                                elschemes(rownum, 1) = elyear
                                elschemes(rownum, 2) = schemearray(RlElNum, 2)
                                elschemes(rownum, 3) = schemearray(RlElNum, 3)
                                'subtract the electrified km from the spare km
                                kmtoelectrify = kmtoelectrify - eltrackkm
                                schemecount += 1
                                schemecode = schemearray(RlElNum, 4)
                                Do While zonecheck = True
                                    If schemecode = zonearray(RzElNum, 3) Then
                                        elzschemes(znum, 0) = zonearray(RzElNum, 0)
                                        elzschemes(znum, 1) = elyear
                                        elzschemes(znum, 2) = zonearray(RzElNum, 2)
                                        RzElNum += 1
                                        If zonearray(RzElNum, 0) Is Nothing Then
                                            zonecheck = False
                                        Else
                                            znum += 1
                                            zonecheck = True
                                        End If
                                    Else
                                        zonecheck = False
                                    End If
                                Loop
                            Else
                                Exit Do
                            End If
                        End If
                    Else
                        'if it is then load values into array
                        For v = 0 To 3
                            elschemes(rownum, v) = schemearray(RlElNum, v)
                        Next
                        elyear = schemearray(RlElNum, 1)
                        schemecount += 1
                        schemecode = schemearray(RlElNum, 4)
                        Do While zonecheck = True
                            If schemecode = zonearray(RzElNum, 3) Then
                                elzschemes(znum, 0) = zonearray(RzElNum, 0)
                                elzschemes(znum, 1) = zonearray(RzElNum, 1)
                                elzschemes(znum, 2) = zonearray(RzElNum, 2)
                                RzElNum += 1
                                If zonearray(RzElNum, 0) Is Nothing Then
                                    zonecheck = False
                                Else
                                    znum += 1
                                    zonecheck = True
                                End If
                            Else
                                zonecheck = False
                            End If
                        Loop
                    End If
                End If
            End If
            'read next line from input file
            RlElNum += 1
            rownum += 1
            zonecheck = True
        Loop
        'now need to sort the array by flow id then by year
        ReDim sortarray(schemecount)
        For v = 0 To schemecount
            'concatenate the relevant values (first flow id, then year, then main array position) into a single dimension string array
            padflow = String.Format("{0:000}", elschemes(v, 0))
            padyear = String.Format("{0:00}", elschemes(v, 1))
            sortarray(v) = padyear & "&" & padflow & "&" & v
        Next
        'sort this array
        Array.Sort(sortarray)
        'then go through the sorted values getting the relevant information from the main array and writing to the output file
        RlElNum = 1
        For v = 0 To schemecount
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            'skip lines which don't correspond to a flow
            If elschemes(arraynum, 0) > 0 Then
                schemeoutputrow(RlElNum, 0) = elschemes(arraynum, 1)
                schemeoutputrow(RlElNum, 1) = elschemes(arraynum, 0)
                schemeoutputrow(RlElNum, 2) = elschemes(arraynum, 2)
                schemeoutputrow(RlElNum, 3) = elschemes(arraynum, 3)
                RlElNum += 1
            End If
        Next
        'now need to sort the zone array by zone id then by year
        ReDim sortarray(znum - 1)
        For v = 0 To (znum - 1)
            'concatenate the relevant values (first zone id, then year, then main array position) into a single dimension string array
            padflow = String.Format("{0:000}", elzschemes(v, 0))
            padyear = String.Format("{0:00}", elzschemes(v, 1))
            sortarray(v) = padyear & "&" & padflow & "&" & v
        Next
        'sort this array
        Array.Sort(sortarray)
        'then go through the sorted values getting the relevant information from the main array and writing to the output file
        RzElNum = 1
        For v = 0 To (znum - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            'skip lines which have a zero station count
            If elzschemes(arraynum, 2) > 0 Then
                zoneoutputrow(RzElNum, 0) = elzschemes(arraynum, 1)
                zoneoutputrow(RzElNum, 1) = elzschemes(arraynum, 0)
                zoneoutputrow(RzElNum, 2) = elzschemes(arraynum, 2)
                RzElNum += 1
            End If
        Next

        Call WriteData("RailLink", "RlLinkElSchemes", schemeoutputrow)
        Call WriteData("RailLink", "RlZoneElSchemes", zoneoutputrow)

    End Sub

    Sub GetElectData()

        If elearray(EleNum, 0) = "" Then
            Elect = False
        Else
            ElectFlow = elearray(EleNum, 1)
            ElectYear = elearray(EleNum, 0) - 2010
            ElectTracks = elearray(EleNum, 2)
            EleNum += 1
        End If
    End Sub
End Module
