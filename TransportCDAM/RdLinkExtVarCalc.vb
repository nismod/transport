Module RdLinkExtVarCalc1pt5
    'creates an external variables file for the road model, based on a single year's input data and growth factors for the other variables
    '1.2 this version allows capacity changes to be specified
    '1.3 this version can cope with database input files
    '1.3 also now breaks down and calculates the cost variable
    '1.4 corrects the cost calculations - previously was referring to the wrong place in the strategy file
    '1.5 corrects the fuel efficiency calculations

    Dim RoadInputData As IO.FileStream
    Dim ri As IO.StreamReader
    Dim ExtVarOutputData As IO.FileStream
    Dim ev As IO.StreamWriter
    Dim RoadLinkCapData As IO.FileStream
    Dim rc As IO.StreamReader
    Dim stf As IO.StreamReader
    Dim RoadLinkNewCapData As IO.FileStream
    Dim rnc As IO.StreamWriter
    Dim rncr As IO.StreamReader
    Dim InputRow As String
    Dim OutputRow As String
    Dim InputData() As String
    Dim PopGrowth As Double
    Dim GVAGrowth As Double
    Dim CostGrowth As Double
    Dim CapID As Long
    Dim CapYear, CapNewYear As Integer
    Dim MLaneChange As Integer
    Dim DLaneChange As Integer
    Dim SLaneChange As Integer
    Dim ErrorString As String
    Dim OutString As String
    Dim CapCount As Double
    Dim AddingCap As Boolean
    Dim LanesToBuild, CapLanes As Double
    Dim CapType, CapRow As String
    Dim NewCapDetails(6834, 5) As Double
    Dim Breakout As Boolean
    Dim sortarray(6834) As String
    Dim sortedline As String
    Dim splitline() As String
    Dim arraynum As Long
    Dim padflow, padyear As String
    Dim FuelEffOld(34), FuelEffNew(34), FuelEffChange(34) As Double

    Public Sub RoadLinkEVMain()

        Dim InputCount As Long

        'get the input and output file names
        Call GetFiles()

        'read header row
        InputRow = ri.ReadLine

        'write header row to output file
        OutputRow = "FlowID,Yeary,PopZ1y,PopZ2y,GVAZ1y,GVAZ2y,M1Costy,MLanesy,DLanesy,SLanesy,MaxCapMy,MaxCapDy,MaxCapSy,M2Costy,M3Costy,M4Costy,M5Costy,M6Costy,D1Costy,D2Costy,D3Costy,D4Costy,D5Costy,D6Costy,S1Costy,S2Costy,S3Costy,S4Costy,S5Costy,S6Costy,S7Costy,S8Costy"
        ev.WriteLine(OutputRow)

        'if we are using a single scaling factor then set scaling factors - as a default they are just set to be constant over time
        If RdLPopSource = "Constant" Then
            PopGrowth = 1.005
        End If
        If RdLEcoSource = "Constant" Then
            GVAGrowth = 1.01
        End If
        If RdLEneSource = "Constant" Then
            CostGrowth = 1.01
        End If

        InputCount = 1

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.4 change - now read this anyway to deal with compulsory enhancements
        'so we created another file containing sorted implemented capacity enhancements (in get files sub)
        'need initial file to be sorted by file type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array
        CapRow = rc.ReadLine
        CapCount = 0
        AddingCap = False
        LanesToBuild = 0
        Do Until CapRow Is Nothing
            Call GetCapData()
            Select Case CapType
                Case "C"
                    NewCapDetails(CapCount, 0) = CapID
                    NewCapDetails(CapCount, 1) = CapYear
                    NewCapDetails(CapCount, 2) = MLaneChange
                    NewCapDetails(CapCount, 3) = DLaneChange
                    NewCapDetails(CapCount, 4) = SLaneChange
                    CapNewYear = CapYear
                Case "O"
                    'then if adding optional capacity read all optional dated enhancements to intermediate array
                    If NewRdLCap = True Then
                        If CapYear >= 0 Then
                            NewCapDetails(CapCount, 0) = CapID
                            NewCapDetails(CapCount, 1) = CapYear
                            NewCapDetails(CapCount, 2) = MLaneChange
                            NewCapDetails(CapCount, 3) = DLaneChange
                            NewCapDetails(CapCount, 4) = SLaneChange
                            CapNewYear = CapYear
                        Else
                            'finally add all other enhancements to intermediate array until we have run out of additional capacity
                            CapLanes = MLaneChange + DLaneChange + SLaneChange
                            If LanesToBuild >= CapLanes Then
                                NewCapDetails(CapCount, 0) = CapID
                                NewCapDetails(CapCount, 1) = CapNewYear
                                NewCapDetails(CapCount, 2) = MLaneChange
                                NewCapDetails(CapCount, 3) = DLaneChange
                                NewCapDetails(CapCount, 4) = SLaneChange
                                LanesToBuild = LanesToBuild - CapLanes
                            Else
                                Do Until LanesToBuild >= CapLanes
                                    CapNewYear += 1
                                    If CapNewYear > 90 Then
                                        Breakout = True
                                        Exit Select
                                    End If
                                    LanesToBuild += NewRoadLanes
                                Loop
                                NewCapDetails(CapCount, 0) = CapID
                                NewCapDetails(CapCount, 1) = CapNewYear
                                NewCapDetails(CapCount, 2) = MLaneChange
                                NewCapDetails(CapCount, 3) = DLaneChange
                                NewCapDetails(CapCount, 4) = SLaneChange
                                LanesToBuild = LanesToBuild - CapLanes
                            End If
                        End If
                    Else
                        Exit Do
                    End If
            End Select
            If Breakout = True Then
                Exit Do
            End If
            CapRow = rc.ReadLine
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
        ReDim RLCapYear(CapCount)
        'write all lines to intermediate capacity file
        For v = 0 To (CapCount - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            OutputRow = NewCapDetails(arraynum, 0) & "," & NewCapDetails(arraynum, 1) & "," & NewCapDetails(arraynum, 2) & "," & NewCapDetails(arraynum, 3) & "," & NewCapDetails(arraynum, 4)
            RLCapYear(v) = NewCapDetails(arraynum, 1)
            rnc.WriteLine(OutputRow)
        Next

        'this variable is needed to select new capacity for the interzonal model
        RoadCapNum = CapCount

        rc.Close()
        rnc.Close()

        'reopen the capacity file as a reader
        RoadLinkNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "RoadLinkNewCap.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rncr = New IO.StreamReader(RoadLinkNewCapData, System.Text.Encoding.Default)
        'read header
        rncr.ReadLine()
        'read first line of new capacity
        CapRow = rncr.ReadLine
        AddingCap = True
        Call GetCapData()

        'If NewRdLCap = True Then
        '    Call GetCapData()
        'End If

        'then loop through rest of rows in input data file
        Do Until InputCount > 291
            '1.3 get the strategy file
            'open the strategy file
            StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
            stf = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
            'read header row
            stf.ReadLine()
            Call CalcFlowData()
            InputCount += 1
            stf.Close()
        Loop

        ri.Close()
        ev.Close()
        rncr.Close()

    End Sub

    Sub GetFiles()

        RoadInputData = New IO.FileStream(DirPath & "RoadInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        ri = New IO.StreamReader(RoadInputData, System.Text.Encoding.Default)

        ExtVarOutputData = New IO.FileStream(DirPath & EVFilePrefix & "ExternalVariables.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        ev = New IO.StreamWriter(ExtVarOutputData, System.Text.Encoding.Default)

        'if capacity is changing then get capacity change file
        'v1.3 do this anyway to include compulsory changes
        RoadLinkCapData = New IO.FileStream(DirPath & CapFilePrefix & "RoadLinkCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rc = New IO.StreamReader(RoadLinkCapData, System.Text.Encoding.Default)
        'read header row
        rc.ReadLine()
        'If NewRdLCap = True Then
        '    RoadLinkCapData = New IO.FileStream(DirPath & CapFilePrefix & "RoadLinkCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        '    rc = New IO.StreamReader(RoadLinkCapData, System.Text.Encoding.Default)
        '    'read header row
        '    rc.ReadLine()
        'End If
        'v1.4 new intermediate capacity file
        RoadLinkNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "RoadLinkNewCap.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rnc = New IO.StreamWriter(RoadLinkNewCapData, System.Text.Encoding.Default)
        'writeheaderrow
        OutString = "FlowID,ChangeYear,MLaneChange,DLaneChange,SLaneChange"
        rnc.WriteLine(OutString)

    End Sub

    Sub CalcFlowData()

        Dim FlowID As String
        Dim Year As Long
        Dim Pop1Old As Double
        Dim Pop2Old As Double
        Dim GVA1Old As Double
        Dim GVA2Old As Double
        Dim Pop1New As Double
        Dim Pop2New As Double
        Dim GVA1New As Double
        Dim GVA2New As Double
        Dim MLanes As Long
        Dim DLanes As Long
        Dim SLanes As Long
        Dim MCap, DCap, SCap As Double
        Dim OZone, DZone As Long
        Dim keylookup As String
        Dim newval As Double
        Dim CostsOld(19) As Double
        Dim CostsNew(19) As Double
        Dim countvar As Integer
        Dim enestring As String
        Dim enearray() As String
        Dim PetOld, PetNew, DieOld, DieNew, EleOld, EleNew, LPGOld, LPGNew, CNGOld, CNGNew, HydOld, HydNew As Double
        Dim PetRat, DieRat, EleRat, LPGRat, CNGRat, HydRat As Double
        Dim VehCosts(19, 9) As Double
        Dim FuelCostPer(19, 9) As Double
        Dim VehFixedCosts(19, 9) As Double
        Dim VehFuelCosts(19, 9) As Double
        Dim CarbCharge(19, 9) As Double
        Dim stratstring As String
        Dim stratarray() As String

        InputRow = ri.ReadLine
        InputData = Split(InputRow, ",")
        FlowID = InputData(0)
        OZone = InputData(1)
        DZone = InputData(2)
        Pop1Old = InputData(28)
        Pop2Old = InputData(29)
        GVA1Old = InputData(30)
        GVA2Old = InputData(31)
        CostsOld(0) = InputData(32)
        MLanes = InputData(4)
        DLanes = InputData(5)
        SLanes = InputData(6)
        MCap = InputData(33)
        DCap = InputData(34)
        SCap = InputData(35)
        countvar = 36
        For x = 1 To 19
            CostsOld(x) = CDbl(InputData(countvar))
            countvar += 1
        Next

        If RdLEneSource = "Database" Then
            'v1.4 altered so that scenario file is read directly as an input file
            ZoneEneFile = New IO.FileStream(DBaseEneFile, IO.FileMode.Open, IO.FileAccess.Read)
            zer = New IO.StreamReader(ZoneEneFile, System.Text.Encoding.Default)
            'read header row
            enestring = zer.ReadLine
            'read first line of data
            enestring = zer.ReadLine
            enearray = Split(enestring, ",")
            PetOld = enearray(1)
            DieOld = enearray(2)
            EleOld = enearray(3)
            LPGOld = enearray(4)
            CNGOld = enearray(5)
            HydOld = enearray(6)

            'set base levels of fixed and fuel costs = the first array value is the speed category
            'fixed costs
            For x = 0 To 9
                VehFixedCosts(0, x) = 0.6936 * 26.604
            Next
            VehFixedCosts(1, 0) = 0.7663 * 36.14
            VehFixedCosts(1, 1) = 0.8089 * 36.873
            VehFixedCosts(1, 2) = 0.7663 * 36.14
            VehFixedCosts(1, 3) = 0.8089 * 36.873
            For x = 4 To 9
                VehFixedCosts(1, x) = 0.7663 * 36.14
            Next
            For x = 0 To 9
                VehFixedCosts(2, x) = 0.845 * 61.329
                VehFixedCosts(3, x) = 0.8699 * 234.5
                VehFixedCosts(4, x) = 0.7791 * 93.665
                VehFixedCosts(5, x) = 0.7065 * 109.948
                VehFixedCosts(6, x) = 0.6936 * 26.604
            Next
            VehFixedCosts(7, 0) = 0.7663 * 36.14
            VehFixedCosts(7, 1) = 0.8089 * 36.873
            VehFixedCosts(7, 2) = 0.7663 * 36.14
            VehFixedCosts(7, 3) = 0.8089 * 36.873
            For x = 4 To 9
                VehFixedCosts(7, x) = 0.7663 * 36.14
            Next
            For x = 0 To 9
                VehFixedCosts(8, x) = 0.845 * 61.329
                VehFixedCosts(9, x) = 0.8699 * 234.5
                VehFixedCosts(10, x) = 0.7791 * 93.665
                VehFixedCosts(11, x) = 0.7065 * 109.948
                VehFixedCosts(12, x) = 0.6936 * 26.604
            Next
            VehFixedCosts(13, 0) = 0.7663 * 36.14
            VehFixedCosts(13, 1) = 0.8089 * 36.873
            VehFixedCosts(13, 2) = 0.7663 * 36.14
            VehFixedCosts(13, 3) = 0.8089 * 36.873
            For x = 4 To 9
                VehFixedCosts(13, x) = 0.7663 * 36.14
            Next
            For x = 0 To 9
                VehFixedCosts(14, x) = 0.845 * 61.329
                VehFixedCosts(15, x) = 0.8699 * 234.5
                VehFixedCosts(16, x) = 0.7791 * 93.665
                VehFixedCosts(17, x) = 0.7791 * 93.665
                VehFixedCosts(18, x) = 0.7065 * 109.948
                VehFixedCosts(19, x) = 0.7065 * 109.948
            Next
            'fuel costs
            VehFuelCosts(0, 0) = 0.3064 * 26.604
            'calculation is: ((old el price / old petrol price) * (kwh/100km/l petrol/100km)) * %variable costs) / (%fixed costs + ((old el price / old petrol price) * (kwh/100km/l petrol/100km) * %variable costs)) * total base costs 
            VehFuelCosts(0, 5) = ((EleOld / PetOld) * (3.2 / 4) * 0.3064) / (0.6936 + ((EleOld / PetOld) * (3.2 / 4) * 0.3064)) * 26.604
            VehFuelCosts(0, 9) = ((HydOld / PetOld) * (7.6 / 4) * 0.3064) / (0.6936 + ((HydOld / PetOld) * (7.6 / 4) * 0.3064)) * 26.604
            VehFuelCosts(1, 0) = 0.2337 * 36.14
            VehFuelCosts(1, 1) = 0.1911 * 36.873
            VehFuelCosts(1, 2) = ((11.2 / 18.6) * 0.2337) / (0.7663 + ((11.2 / 18.6) * 0.2337)) * 36.14
            VehFuelCosts(1, 3) = ((7.6 / 12.4) * 0.1911) / (0.8089 + ((7.6 / 12.4) * 0.1911)) * 36.873
            'plug in hybrids use petrol on motorways
            VehFuelCosts(1, 4) = ((18.1 / 25.9) * 0.2337) / (0.7663 + ((18.1 / 25.9) * 0.2337)) * 36.14
            VehFuelCosts(1, 5) = ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337) / (0.7663 + ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337)) * 36.14
            VehFuelCosts(1, 8) = ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337)) * 36.14
            VehFuelCosts(1, 9) = ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337)) * 36.14
            VehFuelCosts(2, 0) = 0.155 * 61.329
            VehFuelCosts(2, 1) = 0.155 * 61.329
            VehFuelCosts(2, 3) = ((4.4 / 7.9) * 0.155) / (0.845 + ((4.4 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(2, 4) = ((5.8 / 7.9) * 0.155) / (0.845 + ((5.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(2, 5) = ((EleOld / DieOld) * (56.2 / 7.9) * 0.155) / (0.845 + ((EleOld / DieOld) * (56.2 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(2, 6) = ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155) / (0.845 + ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(2, 7) = ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155) / (0.845 + ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(3, 1) = 0.1301 * 234.5
            VehFuelCosts(3, 3) = ((30.4 / 37.2) * 0.1301) / (0.8699 + ((30.4 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(3, 4) = ((11.9 / 19.6) * 0.1301) / (0.8699 + ((11.9 / 19.6) * 0.1301)) * 234.5
            VehFuelCosts(3, 5) = ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301) / (0.8699 + ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(3, 6) = ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301) / (0.8699 + ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(3, 7) = ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301) / (0.8699 + ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(3, 9) = ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301) / (0.8699 + ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(4, 1) = 0.2209 * 93.665
            VehFuelCosts(4, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(4, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(4, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(5, 1) = 0.2935 * 109.948
            VehFuelCosts(5, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(5, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(5, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(6, 0) = 0.3064 * 26.604
            VehFuelCosts(6, 5) = ((EleOld / PetOld) * (3.2 / 4) * 0.3064) / (0.6936 + ((EleOld / PetOld) * (3.2 / 4) * 0.3064)) * 26.604
            VehFuelCosts(6, 9) = ((HydOld / PetOld) * (7.6 / 4) * 0.3064) / (0.6936 + ((HydOld / PetOld) * (7.6 / 4) * 0.3064)) * 26.604
            VehFuelCosts(7, 0) = 0.2337 * 36.14
            VehFuelCosts(7, 1) = 0.1911 * 36.873
            VehFuelCosts(7, 2) = ((11.2 / 18.6) * 0.2337) / (0.7663 + ((11.2 / 18.6) * 0.2337)) * 36.14
            VehFuelCosts(7, 3) = ((7.6 / 12.4) * 0.1911) / (0.8089 + ((7.6 / 12.4) * 0.1911)) * 36.873
            'plug in hybrids use petrol on dual carriageways
            VehFuelCosts(7, 4) = ((18.1 / 25.9) * 0.2337) / (0.7663 + ((18.1 / 25.9) * 0.2337)) * 36.14
            VehFuelCosts(7, 5) = ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337) / (0.7663 + ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337)) * 36.14
            VehFuelCosts(7, 8) = ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337)) * 36.14
            VehFuelCosts(7, 9) = ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337)) * 36.14
            VehFuelCosts(8, 0) = 0.155 * 61.329
            VehFuelCosts(8, 1) = 0.155 * 61.329
            VehFuelCosts(8, 3) = ((4.4 / 7.9) * 0.155) / (0.845 + ((4.4 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(8, 4) = ((5.8 / 7.9) * 0.155) / (0.845 + ((5.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(8, 5) = ((EleOld / DieOld) * (56.2 / 7.9) * 0.155) / (0.845 + ((EleOld / DieOld) * (56.2 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(8, 6) = ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155) / (0.845 + ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(8, 7) = ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155) / (0.845 + ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(9, 1) = 0.1301 * 234.5
            VehFuelCosts(9, 3) = ((30.4 / 37.2) * 0.1301) / (0.8699 + ((30.4 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(9, 4) = ((11.9 / 19.6) * 0.1301) / (0.8699 + ((11.9 / 19.6) * 0.1301)) * 234.5
            VehFuelCosts(9, 5) = ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301) / (0.8699 + ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(9, 6) = ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301) / (0.8699 + ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(9, 7) = ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301) / (0.8699 + ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(9, 9) = ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301) / (0.8699 + ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(10, 1) = 0.2209 * 93.665
            VehFuelCosts(10, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(10, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(10, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(11, 1) = 0.2935 * 109.948
            VehFuelCosts(11, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(11, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(11, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(12, 0) = 0.3064 * 26.604
            VehFuelCosts(12, 5) = ((EleOld / PetOld) * (3.2 / 4) * 0.3064) / (0.6936 + ((EleOld / PetOld) * (3.2 / 4) * 0.3064)) * 26.604
            VehFuelCosts(12, 9) = ((HydOld / PetOld) * (7.6 / 4) * 0.3064) / (0.6936 + ((HydOld / PetOld) * (7.6 / 4) * 0.3064)) * 26.604
            VehFuelCosts(13, 0) = 0.2337 * 36.14
            VehFuelCosts(13, 1) = 0.1911 * 36.873
            VehFuelCosts(13, 2) = ((11.2 / 18.6) * 0.2337) / (0.7663 + ((11.2 / 18.6) * 0.2337)) * 36.14
            VehFuelCosts(13, 3) = ((7.6 / 12.4) * 0.1911) / (0.8089 + ((7.6 / 12.4) * 0.1911)) * 36.873
            'model assumes for this purpose that plug in hybrids use petrol on single carriageways
            VehFuelCosts(13, 4) = ((18.1 / 25.9) * 0.2337) / (0.7663 + ((18.1 / 25.9) * 0.2337)) * 36.14
            VehFuelCosts(13, 5) = ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337) / (0.7663 + ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337)) * 36.14
            VehFuelCosts(13, 8) = ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337)) * 36.14
            VehFuelCosts(13, 9) = ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337)) * 36.14
            VehFuelCosts(14, 0) = 0.155 * 61.329
            VehFuelCosts(14, 1) = 0.155 * 61.329
            VehFuelCosts(14, 3) = ((4.4 / 7.9) * 0.155) / (0.845 + ((4.4 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(14, 4) = ((5.8 / 7.9) * 0.155) / (0.845 + ((5.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(14, 5) = ((EleOld / DieOld) * (56.2 / 7.9) * 0.155) / (0.845 + ((EleOld / DieOld) * (56.2 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(14, 6) = ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155) / (0.845 + ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(14, 7) = ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155) / (0.845 + ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155)) * 61.329
            VehFuelCosts(15, 1) = 0.1301 * 234.5
            VehFuelCosts(15, 3) = ((30.4 / 37.2) * 0.1301) / (0.8699 + ((30.4 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(15, 4) = ((11.9 / 19.6) * 0.1301) / (0.8699 + ((11.9 / 19.6) * 0.1301)) * 234.5
            VehFuelCosts(15, 5) = ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301) / (0.8699 + ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(15, 6) = ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301) / (0.8699 + ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(15, 7) = ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301) / (0.8699 + ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(15, 9) = ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301) / (0.8699 + ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301)) * 234.5
            VehFuelCosts(16, 1) = 0.2209 * 93.665
            VehFuelCosts(16, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(16, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(16, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(17, 1) = 0.2209 * 93.665
            VehFuelCosts(17, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(17, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(17, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209)) * 93.665
            VehFuelCosts(18, 1) = 0.2935 * 109.948
            VehFuelCosts(18, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(18, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(18, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(19, 1) = 0.2935 * 109.948
            VehFuelCosts(19, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(19, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935)) * 109.948
            VehFuelCosts(19, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935)) * 109.948
        End If

        'Set year as 1 to start with
        Year = 1

        '030114 change set fuel efficiency old values to one
        For f = 0 To 34
            FuelEffOld(f) = 1
        Next

        Do While Year < 91
            'loop through scaling up values for each year and writing to output file until the 90th year
            'read line of strategy file
            stratstring = stf.ReadLine
            stratarray = Split(stratstring, ",")

            If RdLPopSource = "Constant" Then
                Pop1New = Pop1Old * PopGrowth
                Pop2New = Pop2Old * PopGrowth
            ElseIf RdLPopSource = "File" Then
                '***scaling files not currently set up for road links module
            ElseIf RdLPopSource = "Database" Then
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
            If RdLEcoSource = "Constant" Then
                GVA1New = GVA1Old * GVAGrowth
                GVA2New = GVA2Old * GVAGrowth
            ElseIf RdLEcoSource = "File" Then
                '***scaling files not currently set up for road links module
            ElseIf RdLEcoSource = "Database" Then
                'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                'now modified as gva data available up to 2100 - so should never need 'else'
                If Year < 91 Then
                    keylookup = Year & "_" & OZone
                    If EcoYearLookup.TryGetValue(keylookup, newval) Then
                        GVA1New = newval
                    Else
                        ErrorString = "gva found in lookup table for zone " & OZone & "in year " & Year
                        Call DictionaryMissingVal()
                    End If
                    keylookup = Year & "_" & DZone
                    If EcoYearLookup.TryGetValue(keylookup, newval) Then
                        GVA2New = newval
                    Else
                        ErrorString = "gva found in lookup table for zone " & DZone & "in year " & Year
                        Call DictionaryMissingVal()
                    End If
                Else
                    GVA1New = GVA1Old
                    GVA2New = GVA2Old
                End If
            End If

            If RdLEneSource = "Constant" Then
                For x = 0 To 19
                    CostsNew(x) = CostsOld(x) * CostGrowth
                Next
            ElseIf RdLEneSource = "File" Then
                'not set up for scaling files
            ElseIf RdLEneSource = "Database" Then
                'read in new fuel values
                enestring = zer.ReadLine
                enearray = Split(enestring, ",")
                PetNew = enearray(1)
                DieNew = enearray(2)
                EleNew = enearray(3)
                LPGNew = enearray(4)
                CNGNew = enearray(5)
                HydNew = enearray(6)
                'calculate ratio for each fuel
                PetRat = PetNew / PetOld
                DieRat = DieNew / DieOld
                EleRat = EleNew / EleOld
                LPGRat = LPGNew / LPGOld
                CNGRat = CNGNew / CNGOld
                HydRat = HydNew / HydOld
                '0301014 corrected fuel efficiency change calculation - was previously just multiplying by figure straight from strategy array (which meant that fuel costs quickly declined to zero)
                For f = 0 To 34
                    FuelEffNew(f) = stratarray(f + 31)
                    FuelEffChange(f) = FuelEffNew(f) / FuelEffOld(f)
                Next
                'calculate cost for each vehicle type - these are the 19 speed categories
                'calculate new cost for each fuel type within each vehicle type - 0 is petrol, 1 is diesel, 2 is petrol hybrid, 3 is diesel hybrid, 4 is plug-in hybrid, 5 is battery electric,
                '...6 is LPG, 7 is CNG, 8 is hydrogen IC, 9 is hydrogen fuel cell - by multiplying the fuel cost by the fuel ratio
                'the cost is also multiplied by changes in fuel efficiency
                VehFuelCosts(0, 0) = VehFuelCosts(0, 0) * PetRat * FuelEffChange(32)
                VehFuelCosts(0, 5) = VehFuelCosts(0, 5) * EleRat * FuelEffChange(33)
                VehFuelCosts(0, 9) = VehFuelCosts(0, 9) * HydRat * FuelEffChange(34)
                VehFuelCosts(1, 0) = VehFuelCosts(1, 0) * PetRat * FuelEffChange(0)
                VehFuelCosts(1, 1) = VehFuelCosts(1, 1) * DieRat * FuelEffChange(1)
                VehFuelCosts(1, 2) = VehFuelCosts(1, 2) * PetRat * FuelEffChange(12)
                VehFuelCosts(1, 3) = VehFuelCosts(1, 3) * DieRat * FuelEffChange(13)
                VehFuelCosts(1, 4) = VehFuelCosts(1, 4) * PetRat * FuelEffChange(14)
                VehFuelCosts(1, 5) = VehFuelCosts(1, 5) * EleRat * FuelEffChange(2)
                VehFuelCosts(1, 8) = VehFuelCosts(1, 8) * HydRat * FuelEffChange(15)
                VehFuelCosts(1, 9) = VehFuelCosts(1, 9) * HydRat * FuelEffChange(16)
                VehFuelCosts(2, 0) = VehFuelCosts(2, 0) * PetRat * FuelEffChange(3)
                VehFuelCosts(2, 1) = VehFuelCosts(2, 1) * PetRat * FuelEffChange(4)
                VehFuelCosts(2, 3) = VehFuelCosts(2, 3) * DieRat * FuelEffChange(17)
                VehFuelCosts(2, 4) = VehFuelCosts(2, 4) * DieRat * FuelEffChange(18)
                VehFuelCosts(2, 5) = VehFuelCosts(2, 5) * EleRat * FuelEffChange(5)
                VehFuelCosts(2, 6) = VehFuelCosts(2, 6) * LPGRat * FuelEffChange(19)
                VehFuelCosts(2, 7) = VehFuelCosts(2, 7) * CNGRat * FuelEffChange(20)
                VehFuelCosts(3, 1) = VehFuelCosts(3, 1) * DieRat * FuelEffChange(10)
                VehFuelCosts(3, 3) = VehFuelCosts(3, 3) * DieRat * FuelEffChange(21)
                VehFuelCosts(3, 4) = VehFuelCosts(3, 4) * DieRat * FuelEffChange(22)
                VehFuelCosts(3, 5) = VehFuelCosts(3, 5) * EleRat * FuelEffChange(11)
                VehFuelCosts(3, 6) = VehFuelCosts(3, 6) * LPGRat * FuelEffChange(23)
                VehFuelCosts(3, 7) = VehFuelCosts(3, 7) * CNGRat * FuelEffChange(24)
                VehFuelCosts(3, 9) = VehFuelCosts(3, 9) * HydRat * FuelEffChange(25)
                VehFuelCosts(4, 1) = VehFuelCosts(4, 1) * DieRat * FuelEffChange(6)
                VehFuelCosts(4, 3) = VehFuelCosts(4, 3) * DieRat * FuelEffChange(26)
                VehFuelCosts(4, 8) = VehFuelCosts(4, 8) * HydRat * FuelEffChange(27)
                VehFuelCosts(4, 9) = VehFuelCosts(4, 9) * HydRat * FuelEffChange(28)
                VehFuelCosts(5, 1) = VehFuelCosts(5, 1) * DieRat * FuelEffChange(8)
                VehFuelCosts(5, 3) = VehFuelCosts(5, 3) * DieRat * FuelEffChange(29)
                VehFuelCosts(5, 8) = VehFuelCosts(5, 8) * HydRat * FuelEffChange(30)
                VehFuelCosts(5, 9) = VehFuelCosts(5, 9) * HydRat * FuelEffChange(31)
                VehFuelCosts(6, 0) = VehFuelCosts(6, 0) * PetRat * FuelEffChange(32)
                VehFuelCosts(6, 5) = VehFuelCosts(6, 5) * EleRat * FuelEffChange(33)
                VehFuelCosts(6, 9) = VehFuelCosts(6, 9) * HydRat * FuelEffChange(34)
                VehFuelCosts(7, 0) = VehFuelCosts(7, 0) * PetRat * FuelEffChange(0)
                VehFuelCosts(7, 1) = VehFuelCosts(7, 1) * DieRat * FuelEffChange(1)
                VehFuelCosts(7, 2) = VehFuelCosts(7, 2) * PetRat * FuelEffChange(12)
                VehFuelCosts(7, 3) = VehFuelCosts(7, 3) * DieRat * FuelEffChange(13)
                VehFuelCosts(7, 4) = VehFuelCosts(7, 4) * PetRat * FuelEffChange(14)
                VehFuelCosts(7, 5) = VehFuelCosts(7, 5) * EleRat * FuelEffChange(2)
                VehFuelCosts(7, 8) = VehFuelCosts(7, 8) * HydRat * FuelEffChange(15)
                VehFuelCosts(7, 9) = VehFuelCosts(7, 9) * HydRat * FuelEffChange(16)
                VehFuelCosts(8, 0) = VehFuelCosts(8, 0) * PetRat * FuelEffChange(3)
                VehFuelCosts(8, 1) = VehFuelCosts(8, 1) * PetRat * FuelEffChange(4)
                VehFuelCosts(8, 3) = VehFuelCosts(8, 3) * DieRat * FuelEffChange(17)
                VehFuelCosts(8, 4) = VehFuelCosts(8, 4) * DieRat * FuelEffChange(18)
                VehFuelCosts(8, 5) = VehFuelCosts(8, 5) * EleRat * FuelEffChange(5)
                VehFuelCosts(8, 6) = VehFuelCosts(8, 6) * LPGRat * FuelEffChange(19)
                VehFuelCosts(8, 7) = VehFuelCosts(8, 7) * CNGRat * FuelEffChange(20)
                VehFuelCosts(9, 1) = VehFuelCosts(9, 1) * DieRat * FuelEffChange(10)
                VehFuelCosts(9, 3) = VehFuelCosts(9, 3) * DieRat * FuelEffChange(21)
                VehFuelCosts(9, 4) = VehFuelCosts(9, 4) * DieRat * FuelEffChange(22)
                VehFuelCosts(9, 5) = VehFuelCosts(9, 5) * EleRat * FuelEffChange(11)
                VehFuelCosts(9, 6) = VehFuelCosts(9, 6) * LPGRat * FuelEffChange(23)
                VehFuelCosts(9, 7) = VehFuelCosts(9, 7) * CNGRat * FuelEffChange(24)
                VehFuelCosts(9, 9) = VehFuelCosts(9, 9) * HydRat * FuelEffChange(25)
                VehFuelCosts(10, 1) = VehFuelCosts(10, 1) * DieRat * FuelEffChange(6)
                VehFuelCosts(10, 3) = VehFuelCosts(10, 3) * DieRat * FuelEffChange(26)
                VehFuelCosts(10, 8) = VehFuelCosts(10, 8) * HydRat * FuelEffChange(27)
                VehFuelCosts(10, 9) = VehFuelCosts(10, 9) * HydRat * FuelEffChange(28)
                VehFuelCosts(11, 1) = VehFuelCosts(11, 1) * DieRat * FuelEffChange(8)
                VehFuelCosts(11, 3) = VehFuelCosts(11, 3) * DieRat * FuelEffChange(29)
                VehFuelCosts(11, 8) = VehFuelCosts(11, 8) * HydRat * FuelEffChange(30)
                VehFuelCosts(11, 9) = VehFuelCosts(11, 9) * HydRat * FuelEffChange(31)
                VehFuelCosts(12, 0) = VehFuelCosts(12, 0) * PetRat * FuelEffChange(32)
                VehFuelCosts(12, 5) = VehFuelCosts(12, 5) * EleRat * FuelEffChange(33)
                VehFuelCosts(12, 9) = VehFuelCosts(12, 9) * HydRat * FuelEffChange(34)
                VehFuelCosts(13, 0) = VehFuelCosts(13, 0) * PetRat * FuelEffChange(0)
                VehFuelCosts(13, 1) = VehFuelCosts(13, 1) * DieRat * FuelEffChange(1)
                VehFuelCosts(13, 2) = VehFuelCosts(13, 2) * PetRat * FuelEffChange(12)
                VehFuelCosts(13, 3) = VehFuelCosts(13, 3) * DieRat * FuelEffChange(13)
                VehFuelCosts(13, 4) = VehFuelCosts(13, 4) * PetRat * FuelEffChange(14)
                VehFuelCosts(13, 5) = VehFuelCosts(13, 5) * EleRat * FuelEffChange(2)
                VehFuelCosts(13, 8) = VehFuelCosts(13, 8) * HydRat * FuelEffChange(15)
                VehFuelCosts(13, 9) = VehFuelCosts(13, 9) * HydRat * FuelEffChange(16)
                VehFuelCosts(14, 0) = VehFuelCosts(14, 0) * PetRat * FuelEffChange(3)
                VehFuelCosts(14, 1) = VehFuelCosts(14, 1) * PetRat * FuelEffChange(4)
                VehFuelCosts(14, 3) = VehFuelCosts(14, 3) * DieRat * FuelEffChange(17)
                VehFuelCosts(14, 4) = VehFuelCosts(14, 4) * DieRat * FuelEffChange(18)
                VehFuelCosts(14, 5) = VehFuelCosts(14, 5) * EleRat * FuelEffChange(5)
                VehFuelCosts(14, 6) = VehFuelCosts(14, 6) * LPGRat * FuelEffChange(19)
                VehFuelCosts(14, 7) = VehFuelCosts(14, 7) * CNGRat * FuelEffChange(20)
                VehFuelCosts(15, 1) = VehFuelCosts(15, 1) * DieRat * FuelEffChange(10)
                VehFuelCosts(15, 3) = VehFuelCosts(15, 3) * DieRat * FuelEffChange(21)
                VehFuelCosts(15, 4) = VehFuelCosts(15, 4) * DieRat * FuelEffChange(22)
                VehFuelCosts(15, 5) = VehFuelCosts(15, 5) * EleRat * FuelEffChange(11)
                VehFuelCosts(15, 6) = VehFuelCosts(15, 6) * LPGRat * FuelEffChange(23)
                VehFuelCosts(15, 7) = VehFuelCosts(15, 7) * CNGRat * FuelEffChange(24)
                VehFuelCosts(15, 9) = VehFuelCosts(15, 9) * HydRat * FuelEffChange(25)
                VehFuelCosts(16, 1) = VehFuelCosts(16, 1) * DieRat * FuelEffChange(6)
                VehFuelCosts(16, 3) = VehFuelCosts(16, 3) * DieRat * FuelEffChange(26)
                VehFuelCosts(16, 8) = VehFuelCosts(16, 8) * HydRat * FuelEffChange(27)
                VehFuelCosts(16, 9) = VehFuelCosts(16, 9) * HydRat * FuelEffChange(28)
                VehFuelCosts(17, 1) = VehFuelCosts(17, 1) * DieRat * FuelEffChange(6)
                VehFuelCosts(17, 3) = VehFuelCosts(17, 3) * DieRat * FuelEffChange(26)
                VehFuelCosts(17, 8) = VehFuelCosts(17, 8) * HydRat * FuelEffChange(27)
                VehFuelCosts(17, 9) = VehFuelCosts(17, 9) * HydRat * FuelEffChange(28)
                VehFuelCosts(18, 1) = VehFuelCosts(18, 1) * DieRat * FuelEffChange(8)
                VehFuelCosts(18, 3) = VehFuelCosts(18, 3) * DieRat * FuelEffChange(29)
                VehFuelCosts(18, 8) = VehFuelCosts(18, 8) * HydRat * FuelEffChange(30)
                VehFuelCosts(18, 9) = VehFuelCosts(18, 9) * HydRat * FuelEffChange(31)
                VehFuelCosts(19, 1) = VehFuelCosts(19, 1) * DieRat * FuelEffChange(8)
                VehFuelCosts(19, 3) = VehFuelCosts(19, 3) * DieRat * FuelEffChange(29)
                VehFuelCosts(19, 8) = VehFuelCosts(19, 8) * HydRat * FuelEffChange(30)
                VehFuelCosts(19, 9) = VehFuelCosts(19, 9) * HydRat * FuelEffChange(31)
                'v1.4 if using carbon charge then need to add that, assuming it is after the year of introduction
                If CarbonCharge = True Then
                    If Year >= CarbChargeYear Then
                        'note that we assume base (2010) petrol price of 122.1 p/litre when calculating the base fuel consumption (full calculations from base figures not included in model run)
                        'calculation is: (base fuel units per km * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
                        CarbCharge(0, 0) = (0.04 * stratarray(63) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(0, 5) = (0.032 * stratarray(64) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(0, 9) = (0.123 * stratarray(65) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(1, 0) = (0.086 * stratarray(31) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(1, 1) = (0.057 * stratarray(32) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(1, 2) = (0.056 * stratarray(43) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(1, 3) = (0.038 * stratarray(44) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(1, 4) = (0.06 * stratarray(45) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(1, 5) = (0.165 * stratarray(33) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(1, 8) = (0.438 * stratarray(46) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(1, 9) = (0.178 * stratarray(47) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(2, 0) = (0.088 * stratarray(34) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(2, 1) = (0.079 * stratarray(35) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(2, 3) = (0.044 * stratarray(48) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(2, 4) = (0.058 * stratarray(49) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(2, 5) = (0.562 * stratarray(36) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(2, 6) = (0.118 * stratarray(50) * stratarray(75) * (stratarray(71) / 10))
                        CarbCharge(2, 7) = (0.808 * stratarray(51) * stratarray(76) * (stratarray(71) / 10))
                        CarbCharge(3, 1) = (0.176 * stratarray(41) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(3, 3) = (0.185 * stratarray(52) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(3, 4) = (0.119 * stratarray(53) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(3, 5) = (0.2554 * stratarray(42) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(3, 6) = (0.954 * stratarray(54) * stratarray(75) * (stratarray(71) / 10))
                        CarbCharge(3, 7) = (3.749 * stratarray(55) * stratarray(76) * (stratarray(71) / 10))
                        CarbCharge(3, 9) = (0.546 * stratarray(56) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(4, 1) = (0.259 * stratarray(37) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(4, 3) = (0.15 * stratarray(57) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(4, 8) = (0.957 * stratarray(58) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(4, 9) = (0.898 * stratarray(59) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(5, 1) = (0.376 * stratarray(39) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(5, 3) = (0.221 * stratarray(60) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(5, 8) = (1.398 * stratarray(61) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(5, 9) = (1.123 * stratarray(62) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(6, 0) = (0.04 * stratarray(63) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(6, 5) = (0.032 * stratarray(64) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(6, 9) = (0.123 * stratarray(65) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(7, 0) = (0.086 * stratarray(31) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(7, 1) = (0.057 * stratarray(32) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(7, 2) = (0.056 * stratarray(43) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(7, 3) = (0.038 * stratarray(44) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(7, 4) = (0.06 * stratarray(45) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(7, 5) = (0.165 * stratarray(33) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(7, 8) = (0.438 * stratarray(46) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(7, 9) = (0.178 * stratarray(47) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(8, 0) = (0.088 * stratarray(34) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(8, 1) = (0.079 * stratarray(35) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(8, 3) = (0.044 * stratarray(48) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(8, 4) = (0.058 * stratarray(49) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(8, 5) = (0.562 * stratarray(36) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(8, 6) = (0.118 * stratarray(50) * stratarray(75) * (stratarray(71) / 10))
                        CarbCharge(8, 7) = (0.808 * stratarray(51) * stratarray(76) * (stratarray(71) / 10))
                        CarbCharge(9, 1) = (0.176 * stratarray(41) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(9, 3) = (0.185 * stratarray(52) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(9, 4) = (0.119 * stratarray(53) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(9, 5) = (2.554 * stratarray(42) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(9, 6) = (0.954 * stratarray(54) * stratarray(75) * (stratarray(71) / 10))
                        CarbCharge(9, 7) = (3.749 * stratarray(55) * stratarray(76) * (stratarray(71) / 10))
                        CarbCharge(9, 9) = (0.546 * stratarray(56) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(10, 1) = (0.259 * stratarray(37) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(10, 3) = (0.15 * stratarray(57) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(10, 8) = (0.957 * stratarray(58) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(10, 9) = (0.898 * stratarray(59) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(11, 1) = (0.376 * stratarray(39) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(11, 3) = (0.221 * stratarray(60) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(11, 8) = (1.398 * stratarray(61) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(11, 9) = (1.123 * stratarray(62) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(12, 0) = (0.04 * stratarray(63) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(12, 5) = (0.032 * stratarray(64) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(12, 9) = (0.123 * stratarray(65) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(13, 0) = (0.086 * stratarray(31) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(13, 1) = (0.057 * stratarray(32) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(13, 2) = (0.056 * stratarray(43) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(13, 3) = (0.038 * stratarray(44) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(13, 4) = (0.016 * stratarray(45) * stratarray(72) * (stratarray(70) / 10))
                        CarbCharge(13, 5) = (0.165 * stratarray(33) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(13, 8) = (0.438 * stratarray(46) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(13, 9) = (0.178 * stratarray(47) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(14, 0) = (0.088 * stratarray(34) * stratarray(72) * (stratarray(71) / 10))
                        CarbCharge(14, 1) = (0.079 * stratarray(35) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(14, 3) = (0.044 * stratarray(48) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(14, 4) = (0.423 * stratarray(49) * stratarray(73) * (stratarray(70) / 10))
                        CarbCharge(14, 5) = (0.562 * stratarray(36) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(14, 6) = (0.118 * stratarray(50) * stratarray(75) * (stratarray(71) / 10))
                        CarbCharge(14, 7) = (0.808 * stratarray(51) * stratarray(76) * (stratarray(71) / 10))
                        CarbCharge(15, 1) = (0.196 * stratarray(41) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(15, 3) = (0.119 * stratarray(52) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(15, 4) = (1.037 * stratarray(53) * stratarray(73) * (stratarray(70) / 10))
                        CarbCharge(15, 5) = (1.7 * stratarray(42) * stratarray(74) * (stratarray(70) / 10))
                        CarbCharge(15, 6) = (0.364 * stratarray(54) * stratarray(75) * (stratarray(71) / 10))
                        CarbCharge(15, 7) = (6.283 * stratarray(55) * stratarray(76) * (stratarray(71) / 10))
                        CarbCharge(15, 9) = (0.546 * stratarray(56) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(16, 1) = (0.259 * stratarray(37) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(16, 3) = (0.15 * stratarray(57) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(16, 8) = (0.957 * stratarray(58) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(16, 9) = (0.898 * stratarray(59) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(17, 1) = (0.259 * stratarray(37) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(17, 3) = (0.15 * stratarray(57) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(17, 8) = (0.957 * stratarray(58) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(17, 9) = (0.898 * stratarray(59) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(18, 1) = (0.376 * stratarray(39) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(18, 3) = (0.221 * stratarray(60) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(18, 8) = (1.398 * stratarray(61) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(18, 9) = (1.123 * stratarray(62) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(19, 1) = (0.376 * stratarray(39) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(19, 3) = (0.221 * stratarray(60) * stratarray(73) * (stratarray(71) / 10))
                        CarbCharge(19, 8) = (1.398 * stratarray(61) * stratarray(77) * (stratarray(71) / 10))
                        CarbCharge(19, 9) = (1.123 * stratarray(62) * stratarray(77) * (stratarray(71) / 10))
                    End If
                End If

                'add the fixed costs
                'v1.4 and also add the carbon charge if we are using one
                If CarbonCharge = True Then
                    For x = 0 To 19
                        For y = 0 To 9
                            VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(x, y) + CarbCharge(x, y)
                        Next
                    Next
                Else
                    For x = 0 To 19
                        For y = 0 To 9
                            VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(x, y)
                        Next
                    Next
                End If

                ''add the fixed costs
                'For x = 0 To 4
                '    For y = 0 To 9
                '        VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(x, y)
                '    Next
                'Next
                'then multiply these costs by the proportions of vehicles in each fuel type (from strategy file), and aggregate the cost for each vehicle type
                CostsNew(0) = (VehCosts(0, 0) * stratarray(11)) + (VehCosts(0, 5) * stratarray(12)) + (VehCosts(0, 9) * stratarray(13))
                CostsNew(1) = (VehCosts(1, 0) * stratarray(1)) + (VehCosts(1, 1) * stratarray(2)) + (VehCosts(1, 2) * stratarray(14)) + (VehCosts(1, 3) * stratarray(15)) + (VehCosts(1, 4) * stratarray(16)) + (VehCosts(1, 5) * stratarray(3)) + (VehCosts(1, 8) * stratarray(17)) + (VehCosts(1, 9) * stratarray(18))
                CostsNew(2) = (VehCosts(2, 0) * stratarray(4)) + (VehCosts(2, 1) * stratarray(5)) + (VehCosts(2, 3) * stratarray(19)) + (VehCosts(2, 4) * stratarray(20)) + (VehCosts(2, 5) * stratarray(6)) + (VehCosts(2, 6) * stratarray(21)) + (VehCosts(2, 7) * stratarray(22))
                CostsNew(3) = (VehCosts(3, 1) * stratarray(9)) + (VehCosts(3, 3) * stratarray(23)) + (VehCosts(3, 4) * stratarray(24)) + (VehCosts(3, 5) * stratarray(10)) + (VehCosts(3, 6) * stratarray(25)) + (VehCosts(3, 7) * stratarray(26)) + (VehCosts(3, 9) * stratarray(27))
                CostsNew(4) = (VehCosts(4, 1) * stratarray(7)) + (VehCosts(4, 3) * stratarray(28)) + (VehCosts(4, 8) * stratarray(29)) + (VehCosts(4, 9) * stratarray(30))
                CostsNew(5) = (VehCosts(5, 1) * stratarray(7)) + (VehCosts(5, 3) * stratarray(28)) + (VehCosts(5, 8) * stratarray(29)) + (VehCosts(5, 9) * stratarray(30))
                CostsNew(6) = (VehCosts(6, 0) * stratarray(11)) + (VehCosts(6, 5) * stratarray(12)) + (VehCosts(6, 9) * stratarray(13))
                CostsNew(7) = (VehCosts(7, 0) * stratarray(1)) + (VehCosts(7, 1) * stratarray(2)) + (VehCosts(7, 2) * stratarray(14)) + (VehCosts(7, 3) * stratarray(15)) + (VehCosts(7, 4) * stratarray(16)) + (VehCosts(7, 5) * stratarray(3)) + (VehCosts(7, 8) * stratarray(17)) + (VehCosts(7, 9) * stratarray(18))
                CostsNew(8) = (VehCosts(8, 0) * stratarray(4)) + (VehCosts(8, 1) * stratarray(5)) + (VehCosts(8, 3) * stratarray(19)) + (VehCosts(8, 4) * stratarray(20)) + (VehCosts(8, 5) * stratarray(6)) + (VehCosts(8, 6) * stratarray(21)) + (VehCosts(8, 7) * stratarray(22))
                CostsNew(9) = (VehCosts(9, 1) * stratarray(9)) + (VehCosts(9, 3) * stratarray(23)) + (VehCosts(9, 4) * stratarray(24)) + (VehCosts(9, 5) * stratarray(10)) + (VehCosts(9, 6) * stratarray(25)) + (VehCosts(9, 7) * stratarray(26)) + (VehCosts(9, 9) * stratarray(27))
                CostsNew(10) = (VehCosts(10, 1) * stratarray(7)) + (VehCosts(10, 3) * stratarray(28)) + (VehCosts(10, 8) * stratarray(29)) + (VehCosts(10, 9) * stratarray(30))
                CostsNew(11) = (VehCosts(11, 1) * stratarray(7)) + (VehCosts(11, 3) * stratarray(28)) + (VehCosts(11, 8) * stratarray(29)) + (VehCosts(11, 9) * stratarray(30))
                CostsNew(12) = (VehCosts(12, 0) * stratarray(11)) + (VehCosts(12, 5) * stratarray(12)) + (VehCosts(12, 9) * stratarray(13))
                CostsNew(13) = (VehCosts(13, 0) * stratarray(1)) + (VehCosts(13, 1) * stratarray(2)) + (VehCosts(13, 2) * stratarray(14)) + (VehCosts(13, 3) * stratarray(15)) + (VehCosts(13, 4) * stratarray(16)) + (VehCosts(13, 5) * stratarray(3)) + (VehCosts(13, 8) * stratarray(17)) + (VehCosts(13, 9) * stratarray(18))
                CostsNew(14) = (VehCosts(14, 0) * stratarray(4)) + (VehCosts(14, 1) * stratarray(5)) + (VehCosts(14, 3) * stratarray(19)) + (VehCosts(14, 4) * stratarray(20)) + (VehCosts(14, 5) * stratarray(6)) + (VehCosts(14, 6) * stratarray(21)) + (VehCosts(14, 7) * stratarray(22))
                CostsNew(15) = (VehCosts(15, 1) * stratarray(9)) + (VehCosts(15, 3) * stratarray(23)) + (VehCosts(15, 4) * stratarray(24)) + (VehCosts(15, 5) * stratarray(10)) + (VehCosts(15, 6) * stratarray(25)) + (VehCosts(15, 7) * stratarray(26)) + (VehCosts(15, 9) * stratarray(27))
                CostsNew(16) = (VehCosts(16, 1) * stratarray(7)) + (VehCosts(16, 3) * stratarray(28)) + (VehCosts(16, 8) * stratarray(29)) + (VehCosts(16, 9) * stratarray(30))
                CostsNew(17) = (VehCosts(17, 1) * stratarray(7)) + (VehCosts(17, 3) * stratarray(28)) + (VehCosts(17, 8) * stratarray(29)) + (VehCosts(17, 9) * stratarray(30))
                CostsNew(18) = (VehCosts(18, 1) * stratarray(7)) + (VehCosts(18, 3) * stratarray(28)) + (VehCosts(18, 8) * stratarray(29)) + (VehCosts(18, 9) * stratarray(30))
                CostsNew(19) = (VehCosts(19, 1) * stratarray(7)) + (VehCosts(19, 3) * stratarray(28)) + (VehCosts(19, 8) * stratarray(29)) + (VehCosts(19, 9) * stratarray(30))
            End If

            'if including capacity changes, then check if there are any capacity changes on this flow
            'v1.4 changed to include compulsory capacity changes where construction has already begun
            'all this involves is removing the if newrdlcap = true clause, because this was already accounted for when generating the intermediate file, and adding a lineread above getcapdata because this sub was amended
            If FlowID = CapID Then
                'if there are any capacity changes on this flow, check if there are any capacity changes in this year
                If Year = CapYear Then
                    'if there are, then update the capacity variables, and read in the next row from the capacity file
                    MLanes += MLaneChange
                    DLanes += DLaneChange
                    SLanes += SLaneChange
                    CapRow = rncr.ReadLine()
                    Call GetCapData()
                End If
            End If
            'v1.4 now updates maximum lane capacities from common variables file
            MCap = stratarray(79)
            DCap = stratarray(80)
            SCap = stratarray(81)
            'write to output file
            OutputRow = FlowID & "," & Year & "," & Pop1New & "," & Pop2New & "," & GVA1New & "," & GVA2New & "," & CostsNew(0) & "," & MLanes & "," & DLanes & "," & SLanes & "," & MCap & "," & DCap & "," & SCap
            For x = 1 To 19
                OutputRow = OutputRow & "," & CostsNew(x)
            Next
            ev.WriteLine(OutputRow)
            'set old values as previous new values
            Pop1Old = Pop1New
            Pop2Old = Pop2New
            GVA1Old = GVA1New
            GVA2Old = GVA2New
            PetOld = PetNew
            DieOld = DieNew
            EleOld = EleNew
            LPGOld = LPGNew
            CNGOld = CNGNew
            HydOld = HydNew
            For x = 1 To 19
                CostsOld(x) = CostsNew(x)
            Next
            '030114 change
            For f = 0 To 34
                FuelEffOld(f) = FuelEffNew(f)
            Next

            'update year
            Year += 1
        Loop

        If RdLEneSource = "Database" Then
            zer.Close()
        End If

    End Sub

    Sub GetCapData()
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
            MLaneChange = InputData(2)
            DLaneChange = InputData(3)
            SLaneChange = InputData(4)
            If AddingCap = False Then
                CapType = InputData(5)
            End If
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
