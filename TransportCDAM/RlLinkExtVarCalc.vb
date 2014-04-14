Module RlLinkExtVarCalc1pt5
    'creates an external variables file for the rail model, based on a single year's input data and growth factors for the external variables
    '1.2 this version allows capacity changes to be specified
    '1.2 it now allows external variable growth factors to be taken from an input file and to vary over time
    '1.3 this version allows input from the database
    '1.4 now makes use of a list of electrification schemes
    'and also takes account of changing fuel efficiencies
    'and also gives the option of a carbon-based charge
    'now also includes rail zone electrification

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
    Dim stratstring, stratarray() As String
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

    Public Sub RailLinkEVMain()

        'get the input and output file names
        Call GetFiles()

        'check if there is any value assigned to RlLEVSource - if not then set to constant as default
        If RlLEVSource = "" Then
            RlLEVSource = "Constant"
        End If

        'write header row to output file
        OutputRow = "FlowID,Yeary,Tracksy,PopZ1y,PopZ2y,GVAZ1y,GVAZ2y,Costy,CarFuely,MaxTDy,ElPy,ElTracksy,AddTrainsy"
        rae.WriteLine(OutputRow)

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

        InputCount = 1

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.4 change - now read this anyway to deal with compulsory enhancements
        'so we created another file containing sorted implemented capacity enhancements (in get files sub)
        'need initial file to be sorted by scheme type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array
        CapRow = rac.ReadLine
        CapCount = 0
        AddingCap = False
        TracksToBuild = 0
        Do Until CapRow Is Nothing
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
            CapRow = rac.ReadLine
            CapCount += 1
        Loop
        'then sort the intermediate array by flow ID, then by year of implementation
        ReDim sortarray(CapCount - 1)
        For v = 0 To (CapCount - 1)
            padflow = String.Format("{0:000}", NewCapDetails(v, 0))
            padyear = String.Format("{0:00}", NewCapDetails(v, 1))
            sortarray(v) = padflow & "&" & padyear & "&" & v
        Next
        Array.Sort(sortarray)
        'write all lines to intermediate capacity file
        For v = 0 To (CapCount - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            OutputRow = NewCapDetails(arraynum, 0) & "," & NewCapDetails(arraynum, 1) & "," & NewCapDetails(arraynum, 2) & "," & NewCapDetails(arraynum, 3) & "," & NewCapDetails(arraynum, 4)
            rlnc.WriteLine(OutputRow)
        Next

        rac.Close()
        rlnc.Close()

        'reopen the capacity file as a reader
        RlLinkNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "RailLinkNewCap.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rlnr = New IO.StreamReader(RlLinkNewCapData, System.Text.Encoding.Default)
        'read header
        rlnr.ReadLine()
        'read first line of new capacity
        CapRow = rlnr.ReadLine
        AddingCap = True
        Call GetCapData()

        'If NewRlLCap = True Then
        '    Call GetCapData()
        'End If

        'if including rail electrification then create the intermediate file sorted by flow then by date
        'mod - now do this anyway as some schemes are non-discretionary
        'create intermediate file listing timings of scheme implementations
        Call CreateElectrificationList()
        'read the electrification list file as an input file
        RlLinkElSchemes = New IO.FileStream(DirPath & EVFilePrefix & "RailLinkElectrificationDates.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rel = New IO.StreamReader(RlLinkElSchemes, System.Text.Encoding.Default)
        'read header row
        rel.ReadLine()
        Call GetElectData()

        'v1.4
        'get fuel efficiency and other values from the strategy file
        StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        stf = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
        'read header row
        stf.ReadLine()
        'v1.5 set fuel efficiency old to 1
        FuelEffOld(0) = 1
        FuelEffOld(1) = 1
        'v1.5 fuel efficiency change calculation corrected
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
            MaxTD(y) = stratarray(78)
            'v1.5 update FuelEffOld values
            FuelEffOld(0) = stratarray(66)
            FuelEffOld(1) = stratarray(67)
        Next
        stf.Close()

        'loop through rows in input data file calculating the external variable values
        Do Until InputCount > 238
            Call CalcExtVars()
            InputCount += 1
        Loop

        rai.Close()
        rae.Close()
        rlnr.Close()

    End Sub

    Sub GetFiles()

        RlLinkInputData = New IO.FileStream(DirPath & "RailLinkInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rai = New IO.StreamReader(RlLinkInputData, System.Text.Encoding.Default)
        'read header row
        InputRow = rai.ReadLine

        RlLinkExtVar = New IO.FileStream(DirPath & EVFilePrefix & "RailLinkExtVar.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rae = New IO.StreamWriter(RlLinkExtVar, System.Text.Encoding.Default)

        'if capacity is changing then get capacity change file
        'v1.4 do this anyway to include compulsory changes
        RlLinkCapData = New IO.FileStream(DirPath & CapFilePrefix & "RailLinkCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rac = New IO.StreamReader(RlLinkCapData, System.Text.Encoding.Default)
        'read header row
        rac.ReadLine()
        'v1.4 new intermediate capacity file
        RlLinkNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "RailLinkNewCap.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rlnc = New IO.StreamWriter(RlLinkNewCapData, System.Text.Encoding.Default)
        'write header row
        OutString = "FlowID,ChangeYear,TrackChange,MaxTDChange,TrainChange"
        rlnc.WriteLine(OutString)

        'If NewRlLCap = True Then
        '    RlLinkCapData = New IO.FileStream(DirPath & CapFilePrefix & "RailLinkCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        '    rac = New IO.StreamReader(RlLinkCapData, System.Text.Encoding.Default)
        '    'read header row
        '    rac.ReadLine()
        'End If

    End Sub

    Sub CalcExtVars()

        Dim FlowID As Integer
        Dim Year As Integer
        Dim Tracks As Integer
        Dim Pop1Old As Double
        Dim Pop2Old As Double
        Dim GVA1Old As Double
        Dim GVA2Old As Double
        Dim CostOld As Double
        Dim FuelOld As Double
        Dim Pop1New As Double
        Dim Pop2New As Double
        Dim GVA1New As Double
        Dim GVA2New As Double
        Dim CostNew As Double
        Dim FuelNew As Double
        Dim MaxTDOld, MaxTDNew As Double
        Dim DieselOld, DieselNew, ElectricOld, ElectricNew As Double
        Dim DMaintOld, EMaintOld As Double
        Dim ElPOld, ElPNew As Double
        Dim ScalingRow As String
        Dim ScalingData() As String
        Dim OCountry, DCountry As String
        Dim OZone, DZone As Long
        Dim keylookup As String
        Dim newval As Double
        Dim ElectTracksOld, ElectTracksNew As Double
        Dim InDieselOld, InElectricOld, InDieselNew, InElectricNew
        Dim enestring As String
        Dim enearray() As String
        Dim diecarch, elecarch As Double

        InputRow = rai.ReadLine
        InputData = Split(InputRow, ",")
        FlowID = InputData(0)
        OZone = InputData(1)
        DZone = InputData(2)
        Tracks = InputData(3)
        Pop1Old = InputData(5)
        Pop2Old = InputData(6)
        GVA1Old = InputData(7)
        GVA2Old = InputData(8)
        CostOld = InputData(10)
        FuelOld = InputData(11)
        MaxTDOld = InputData(12)
        ElPOld = InputData(13)
        OCountry = InputData(14)
        DCountry = InputData(15)
        ElectTracksOld = InputData(16)
        NewTrains = 0

        'need to set a base value for the diesel fuel cost for this zone
        If RlLEneSource = "Database" Then
            'v1.4 altered so that scenario file is read directly as an input file
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
        Else
            'can assume that 8.77% of total costs (which in all cases are set to 1) are made up of fuel, and that electric costs 55.3% of diesel price
            '0.0877 = (ElP * DieselPrice * 0.553) + (DP * DieselPrice)
            '0.0877 = DieselPrice((ElP * 0.553) + DP)
            'DieselPrice = 0.0877/(0.553ElP + DP)
            DieselOld = 0.0877 / ((0.553 * ElPOld) + (1 - ElPOld))
            ElectricOld = 0.553 * DieselOld
            'also need to set a base value for the maintenance and lease costs for this zone
            'can assume that 26.62% of total costs (which in all cases are set to 1) are made up of maintenance and leasing, and that electric trains cost 75.8% of diesel trains
            '0.2662 = (ElP * DMaint * 0.758) + (DP * DMaint)
            '0.2662 = DMaint((ElP * 0.758) + DP)
            'DMaint = 0.2662/(0.758ElP + DP)
            DMaintOld = 0.2662 / ((0.758 * ElPOld) + (1 - ElPOld))
            EMaintOld = 0.758 * DMaintOld
        End If

        'set year as 1 to start with
        Year = 1

        'get scaling factor file if we are using one
        If RlLOthSource = "File" Then
            RlLinkEVScale = New IO.FileStream(DirPath & "RailLinkEVScaling.csv", IO.FileMode.Open, IO.FileAccess.Read)
            ras = New IO.StreamReader(RlLinkEVScale, System.Text.Encoding.Default)
            'read header row
            InputRow = ras.ReadLine
        End If

        Do While Year < 91
            'loop through scaling up values for each year and writing to output file

            If RlLOthSource = "File" Then
                'if using scaling factors then read in the scaling factors for this year
                ScalingRow = ras.ReadLine
                ScalingData = Split(ScalingRow, ",")
            End If
            If RlLPopSource = "Constant" Then
                Pop1New = Pop1Old * OPopGrowth
                Pop2New = Pop2Old * DPopGrowth
            ElseIf RlLPopSource = "File" Then
                Select Case OCountry
                    Case "E"
                        OPopGrowth = 1 + ScalingData(1)
                    Case "S"
                        OPopGrowth = 1 + ScalingData(2)
                    Case "W"
                        OPopGrowth = 1 + ScalingData(3)
                End Select
                Select Case DCountry
                    Case "E"
                        DPopGrowth = 1 + ScalingData(1)
                    Case "S"
                        DPopGrowth = 1 + ScalingData(2)
                    Case "W"
                        DPopGrowth = 1 + ScalingData(3)
                End Select
                Pop1New = Pop1Old * OPopGrowth
                Pop2New = Pop2Old * DPopGrowth
            ElseIf RlLPopSource = "Database" Then
                'if year is after 2093 then no population forecasts are available so assume population remains constant
                'now modified as population data available up to 2100 - so should never need 'else'
                If Year < 91 Then
                    keylookup = Year & "_" & OZone
                    If PopYearLookup.TryGetValue(keylookup, newval) Then
                        Pop1New = newval
                    Else
                        ErrorString = "population found in lookup table for zone " & OZone & " in year " & Year
                        Call DictionaryMissingVal()
                    End If
                    keylookup = Year & "_" & DZone
                    If PopYearLookup.TryGetValue(keylookup, newval) Then
                        Pop2New = newval
                    Else
                        ErrorString = "population found in lookup table for zone " & DZone & " in year " & Year
                        Call DictionaryMissingVal()
                    End If
                Else
                    Pop1New = Pop1Old
                    Pop2New = Pop2Old
                End If
            End If
            If RlLEcoSource = "Constant" Then
                GVA1New = GVA1Old * OGVAGrowth
                GVA2New = GVA2Old * DGVAGrowth
            ElseIf RlLEcoSource = "File" Then
                OGVAGrowth = 1 + ScalingData(4)
                DGVAGrowth = 1 + ScalingData(4)
                GVA1New = GVA1Old * OGVAGrowth
                GVA2New = GVA2Old * DGVAGrowth
            ElseIf RlLEcoSource = "Database" Then
                'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                'now modified as GVA data available up to 2100 - so should never need 'else'
                If Year < 91 Then
                    keylookup = Year & "_" & OZone
                    If EcoYearLookup.TryGetValue(keylookup, newval) Then
                        GVA1New = newval
                    Else
                        ErrorString = "GVA found in lookup table for zone " & OZone & " in year " & Year
                        Call DictionaryMissingVal()
                    End If
                    keylookup = Year & "_" & DZone
                    If EcoYearLookup.TryGetValue(keylookup, newval) Then
                        GVA2New = newval
                    Else
                        ErrorString = "GVA found in lookup table for zone " & DZone & " in year " & Year
                        Call DictionaryMissingVal()
                    End If
                Else
                    GVA1New = GVA1Old
                    GVA2New = GVA2Old
                End If
            End If
            'need to leave cost growth factor until we know new proportion of electric/diesel trains
            If RlLOthSource = "File" Then
                'MaxTDGrowth = 1 + ScalingData(7)
                ElPGrowth = ScalingData(8)
            End If

            'check if using list of electrification schemes
            ''mod - now do this anyway as some schemes are non-discretionary
            'If RlElect = True Then
            'check if in correct year for the current scheme
            If Year = ElectYear Then
                'if so check if correct row for the current scheme
                If FlowID = ElectFlow Then
                    'if so, then need to alter proportions of diesel and electric trains
                    ElPNew = ElPOld + (0.9 * ((1 - ElPOld) * (ElectTracks / (Tracks - ElectTracksOld))))
                    ElectTracksNew = ElectTracksOld + ElectTracks
                    'read next scheme from list
                    Call GetElectData()
                Else
                    ElPNew = ElPOld
                    ElectTracksNew = ElectTracksOld
                End If
            Else
                ElPNew = ElPOld
                ElectTracksNew = ElectTracksOld
            End If
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
                DieselNew = DieselOld * (1 + ScalingData(5)) * FuelEff(1, Year)
                ElectricNew = ElectricOld * (1 + ScalingData(6)) * FuelEff(0, Year)
                '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                FuelGrowth = 1 + ScalingData(5)
            ElseIf RlLEneSource = "Constant" Then
                DieselNew = DieselOld * CostGrowth * FuelEff(1, Year)
                ElectricNew = ElectricOld * CostGrowth * FuelEff(0, Year)
            ElseIf RlLEneSource = "Database" Then
                enestring = zer.ReadLine
                enearray = Split(enestring, ",")
                InDieselNew = enearray(2)
                InElectricNew = enearray(3)
                DieselNew = DieselOld * (InDieselNew / InDieselOld) * FuelEff(1, Year)
                ElectricNew = ElectricOld * (InElectricNew / InElectricOld) * FuelEff(0, Year)
                '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                FuelGrowth = InDieselNew / InDieselOld
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
            CostNew = 121.381 + ((DieselNew + diecarch) * (1 - ElPNew)) + ((ElectricNew + elecarch) * ElPNew) + (EMaintOld * ElPNew) + (DMaintOld * (1 - ElPNew))
            'estimate new fuel efficiency for road vehicles
            FuelNew = FuelOld * FuelGrowth
            'if including capacity changes, then check if there are any capacity changes on this flow
            'v1.4 changed to include compulsory capacity changes where construction has already begun
            'all this involves is removing the if newrllcap = true clause, because this was already accounted for when generating the intermediate file, and adding a lineread above getcapdata because this sub was amended

            If FlowID = CapID Then
                'if there are any capacity changes on this flow, check if there are any capacity changes in this year
                If Year = CapYear Then
                    'if there are, then update the capacity variables, and read in the next row from the capacity file
                    Tracks += TrackChange
                    'note that MaxTDChange now doesn't work - replaced by strategy common variables file
                    MaxTDOld += MaxTDChange
                    NewTrains = TrainChange
                    CapRow = rlnr.ReadLine()
                    Call GetCapData()
                End If
            End If

            MaxTDNew = MaxTD(Year)
            'write to output file
            OutputRow = FlowID & "," & Year & "," & Tracks & "," & Pop1New & "," & Pop2New & "," & GVA1New & "," & GVA2New & "," & CostNew & "," & FuelNew & "," & MaxTDNew & "," & ElPNew & "," & ElectTracksNew & "," & NewTrains
            rae.WriteLine(OutputRow)
            'set old values as previous new values
            Pop1Old = Pop1New
            Pop2Old = Pop2New
            GVA1Old = GVA1New
            GVA2Old = GVA2New
            CostOld = CostNew
            FuelOld = FuelNew
            MaxTDOld = MaxTDNew
            ElPOld = ElPNew
            ElectTracksOld = ElectTracksNew
            DieselOld = DieselNew
            ElectricOld = ElectricNew
            If RlLEneSource = "Database" Then
                InDieselOld = InDieselNew
                InElectricOld = InElectricNew
            End If
            NewTrains = 0
            'update year
            Year += 1
        Loop

        If RlLEneSource = "Database" Then
            zer.Close()
        End If

    End Sub

    Sub GetCapData()
        'modified in v1.4
        If CapRow Is Nothing Then
        Else
            InputData = Split(CapRow, ",")
            CapID = InputData(0)
            If InputData(1) = "-1" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = InputData(1) - 2010
                Else
                    CapYear = InputData(1)
                End If
            End If
            TrackChange = InputData(2)
            MaxTDChange = InputData(3)
            TrainChange = InputData(4)
            If AddingCap = False Then
                CapType = InputData(5)
            End If
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
        Dim schemeoutputrow As String
        Dim elschemes(244, 3) As Double
        Dim schemearray() As String
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
        Dim zoneoutputrow As String
        Dim zonearray() As String
        Dim schemecode As String
        Dim elzschemes(335, 2) As Long
        Dim znum As Integer
        Dim zonecheck As Boolean

        SchemeListFile = New IO.FileStream(DirPath & "RailElectrificationSchemes.csv", IO.FileMode.Open, IO.FileAccess.Read)
        slf = New IO.StreamReader(SchemeListFile, System.Text.Encoding.Default)
        'read header row
        schemeinputrow = slf.ReadLine

        ZoneListFile = New IO.FileStream(DirPath & "RailZoneElectrificationSchemes.csv", IO.FileMode.Open, IO.FileAccess.Read)
        zlf = New IO.StreamReader(ZoneListFile, System.Text.Encoding.Default)
        'read header row
        zoneinputrow = zlf.ReadLine

        RlLinkElSchemes = New IO.FileStream(DirPath & EVFilePrefix & "RailLinkElectrificationDates.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        res = New IO.StreamWriter(RlLinkElSchemes, System.Text.Encoding.Default)
        'write header row
        schemeoutputrow = "FlowID,ElectricYear,ElectricTracks,RouteKm"
        res.WriteLine(schemeoutputrow)

        RlZoneELSchemes = New IO.FileStream(DirPath & EVFilePrefix & "RailZoneElectrificationDates.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rze = New IO.StreamWriter(RlZoneELSchemes, System.Text.Encoding.Default)
        'write header row
        zoneoutputrow = "ZoneID,ElectricYear,ElectricStations"
        rze.WriteLine(zoneoutputrow)

        kmtoelectrify = 0

        schemeinputrow = slf.ReadLine
        rownum = 0
        schemecount = -1
        'read details of first zone scheme
        zoneinputrow = zlf.ReadLine
        zonearray = Split(zoneinputrow, ",")
        znum = 0
        zonecheck = True
        'loop through all rows in the initial file assigning a year if schemes don't yet have one and writing values to array
        Do Until schemeinputrow Is Nothing
            'split row into an array
            schemearray = Split(schemeinputrow, ",")
            'check the scheme type
            schemetype = schemearray(5)
            If schemetype = "C" Then
                'if it is a compulsory scheme then load values into array
                For v = 0 To 3
                    elschemes(rownum, v) = schemearray(v)
                Next
                elyear = schemearray(1)
                schemecount += 1
                'now check for relevant zone changes
                schemecode = schemearray(4)
                Do While zonecheck = True
                    If schemecode = zonearray(3) Then
                        elzschemes(znum, 0) = zonearray(0)
                        elzschemes(znum, 1) = zonearray(1)
                        elzschemes(znum, 2) = zonearray(2)
                        zoneinputrow = zlf.ReadLine
                        If zoneinputrow Is Nothing Then
                            zonecheck = False
                        Else
                            zonearray = Split(zoneinputrow, ",")
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
                    If schemearray(1) = "" Then
                        'if it isn't then first get the length of track km for the scheme 
                        eltrackkm = schemearray(2) * schemearray(3)
                        'then check if there are any spare electrification km in the pot
                        If kmtoelectrify >= eltrackkm Then
                            'if there are enough, then assign this scheme to this year and load values into array
                            elschemes(rownum, 0) = schemearray(0)
                            elschemes(rownum, 1) = elyear
                            elschemes(rownum, 2) = schemearray(2)
                            elschemes(rownum, 3) = schemearray(3)
                            'subtract the electrified km from the spare km
                            kmtoelectrify = kmtoelectrify - eltrackkm
                            schemecount += 1
                            schemecode = schemearray(4)
                            Do While zonecheck = True
                                If schemecode = zonearray(3) Then
                                    elzschemes(znum, 0) = zonearray(0)
                                    elzschemes(znum, 1) = elyear
                                    elzschemes(znum, 2) = zonearray(2)
                                    zoneinputrow = zlf.ReadLine
                                    If zoneinputrow Is Nothing Then
                                        zonecheck = False
                                    Else
                                        zonearray = Split(zoneinputrow, ",")
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
                                elschemes(rownum, 0) = schemearray(0)
                                elschemes(rownum, 1) = elyear
                                elschemes(rownum, 2) = schemearray(2)
                                elschemes(rownum, 3) = schemearray(3)
                                'subtract the electrified km from the spare km
                                kmtoelectrify = kmtoelectrify - eltrackkm
                                schemecount += 1
                                schemecode = schemearray(4)
                                Do While zonecheck = True
                                    If schemecode = zonearray(3) Then
                                        elzschemes(znum, 0) = zonearray(0)
                                        elzschemes(znum, 1) = elyear
                                        elzschemes(znum, 2) = zonearray(2)
                                        zoneinputrow = zlf.ReadLine
                                        If zoneinputrow Is Nothing Then
                                            zonecheck = False
                                        Else
                                            zonearray = Split(zoneinputrow, ",")
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
                            elschemes(rownum, v) = schemearray(v)
                        Next
                        elyear = schemearray(1)
                        schemecount += 1
                        schemecode = schemearray(4)
                        Do While zonecheck = True
                            If schemecode = zonearray(3) Then
                                elzschemes(znum, 0) = zonearray(0)
                                elzschemes(znum, 1) = zonearray(1)
                                elzschemes(znum, 2) = zonearray(2)
                                zoneinputrow = zlf.ReadLine
                                If zoneinputrow Is Nothing Then
                                    zonecheck = False
                                Else
                                    zonearray = Split(zoneinputrow, ",")
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
            schemeinputrow = slf.ReadLine
            rownum += 1
            zonecheck = True
        Loop
        'now need to sort the array by flow id then by year
        ReDim sortarray(schemecount)
        For v = 0 To schemecount
            'concatenate the relevant values (first flow id, then year, then main array position) into a single dimension string array
            padflow = String.Format("{0:000}", elschemes(v, 0))
            padyear = String.Format("{0:00}", elschemes(v, 1))
            sortarray(v) = padflow & "&" & padyear & "&" & v
        Next
        'sort this array
        Array.Sort(sortarray)
        'then go through the sorted values getting the relevant information from the main array and writing to the output file
        For v = 0 To schemecount
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            'skip lines which don't correspond to a flow
            If elschemes(arraynum, 0) > 0 Then
                schemeoutputrow = elschemes(arraynum, 0) & "," & elschemes(arraynum, 1) & "," & elschemes(arraynum, 2) & "," & elschemes(arraynum, 3)
                res.WriteLine(schemeoutputrow)
            End If
        Next
        'now need to sort the zone array by zone id then by year
        ReDim sortarray(znum - 1)
        For v = 0 To (znum - 1)
            'concatenate the relevant values (first zone id, then year, then main array position) into a single dimension string array
            padflow = String.Format("{0:000}", elzschemes(v, 0))
            padyear = String.Format("{0:00}", elzschemes(v, 1))
            sortarray(v) = padflow & "&" & padyear & "&" & v
        Next
        'sort this array
        Array.Sort(sortarray)
        'then go through the sorted values getting the relevant information from the main array and writing to the output file
        For v = 0 To (znum - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            'skip lines which have a zero station count
            If elzschemes(arraynum, 2) > 0 Then
                zoneoutputrow = elzschemes(arraynum, 0) & "," & elzschemes(arraynum, 1) & "," & elzschemes(arraynum, 2)
                rze.WriteLine(zoneoutputrow)
            End If
        Next
        slf.Close()
        res.Close()
        zlf.Close()
        rze.Close()
    End Sub

    Sub GetElectData()
        Dim schemeline As String
        Dim schemearray() As String

        schemeline = rel.ReadLine
        schemearray = Split(schemeline, ",")
        ElectFlow = schemearray(0)
        ElectYear = schemearray(1) - 2010
        ElectTracks = schemearray(2)
    End Sub
End Module
