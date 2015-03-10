Module RdLinkExtVarCalc
    'creates an external variables file for the road model, based on a single year's input data and growth factors for the other variables
    '1.2 this version allows capacity changes to be specified
    '1.3 this version can cope with database input files
    '1.3 also now breaks down and calculates the cost variable
    '1.4 corrects the cost calculations - previously was referring to the wrong place in the strategy file
    '1.5 corrects the fuel efficiency calculations
    '1.6 recode to calculate by annual timesteps, parameters' dimensions are increased by one to store for each roadlink to avoid override
    '1.9 now the module can run with database connection and read/write from/to database

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
    Dim FuelEffOld(291, 34), FuelEffNew(291, 34), FuelEffChange(34) As Double

    'CalcFlowData Dim
    Dim InputCount As Long
    Dim FlowID(291, 1) As String
    Dim Year As Long
    Dim Pop1Old(291, 1) As Double
    Dim Pop2Old(291, 1) As Double
    Dim GVA1Old(291, 1) As Double
    Dim GVA2Old(291, 1) As Double
    Dim Pop1New As Double
    Dim Pop2New As Double
    Dim GVA1New As Double
    Dim GVA2New As Double
    Dim MLanes(291, 1) As Long
    Dim DLanes(291, 1) As Long
    Dim SLanes(291, 1) As Long
    Dim MCap(291, 1), DCap(291, 1), SCap(291, 1) As Double
    Dim OZone(291, 1), DZone(291, 1) As Long
    Dim keylookup As String
    Dim newval As Double
    Dim CostsOld(291, 19) As Double
    Dim CostsNew(19) As Double
    Dim countvar As Integer
    Dim enestring As String
    Dim PetOld, PetNew, DieOld, DieNew, EleOld, EleNew, LPGOld, LPGNew, CNGOld, CNGNew, HydOld, HydNew As Double
    Dim PetOldArr(291, 1), DieOldArr(291, 1), EleOldArr(291, 1), LPGOldArr(291, 1), CNGOldArr(291, 1), HydOldArr(291, 1) As Double
    Dim PetRat, DieRat, EleRat, LPGRat, CNGRat, HydRat As Double
    Dim VehCosts(19, 9) As Double
    Dim FuelCostPer(19, 9) As Double
    Dim VehFixedCosts(19, 9) As Double
    Dim VehFuelCosts(291, 19, 9) As Double
    Dim CarbCharge(19, 9) As Double
    Dim stratstring As String
    Dim stratarray(90, 95) As String
    Dim enearray(91, 6) As String
    Dim InputArray(291, 140) As String
    Dim RdLEVInputArray(291, 140) As String
    Dim OutputArray(292, 32) As String
    Dim CapArray(6835, 6) As String
    Dim CapNum As Integer
    Dim NewCapArray(6835, 5) As String



    Public Sub RoadLinkEVMain()


        'get the input and output file names
        Call GetFiles()

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

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.4 change - now read this anyway to deal with compulsory enhancements
        'so we created another file containing sorted implemented capacity enhancements (in get files sub)
        'need initial file to be sorted by file type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array


        'if capacity is changing then get capacity change file
        'v1.3 do this anyway to include compulsory changes
        'now read from database
        If g_modelRunYear = g_initialYear Then
            'read capacity data
            Call ReadData("RoadLink", "CapChange", CapArray, g_modelRunYear)

            'do cap change calculation
            Call CapChangeCalc()

            'write all lines from NewCapArray to intermediate capacity file
            Call WriteData("RoadLink", "NewCap", NewCapArray)

        End If

        'read all required new capacity for the current year
        Call ReadData("RoadLink", "NewCap", CapArray, g_modelRunYear)



        'this variable is needed to select new capacity for the interzonal model
        RoadCapNum = CapCount


        'reset NewCapArray row to the begining
        CapNum = 1

        AddingCap = True
        Call GetCapData()

        'v1.6 now calculate external variables and write output in annual timesteps
        Call CalcFlowData()


    End Sub

    Sub GetFiles()


        'read initial input data
        Call ReadData("RoadLink", "Input", InputArray, g_modelRunYear)

        If g_modelRunYear <> g_initialYear Then
            'read previous year's data
            Call ReadData("RoadLink", "ExtVar", RdLEVInputArray, g_modelRunYear - 1)
        End If


        '1.3 get the strategy file
        'open the strategy file
        Call ReadData("SubStrategy", "", stratarray)

        If RdLEneSource = "Database" Then
            'v1.4 altered so that scenario file is read directly as an input file
            Call ReadData("Energy", "", enearray)
        End If

    End Sub

    Sub CalcFlowData()

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


        InputCount = 1

        Do Until InputCount > 291

            If g_modelRunYear = g_initialYear Then
                'get initial data if it is the initial year
                If RdLEneSource = "Database" Then

                    PetOld = enearray(1, 1)
                    DieOld = enearray(1, 2)
                    EleOld = enearray(1, 3)
                    LPGOld = enearray(1, 4)
                    CNGOld = enearray(1, 5)
                    HydOld = enearray(1, 6)

                    'fuel costs
                    'v1.6 comment
                    'i is road link and there are 291 links in current model
                    'if the number of links are changed, i should be changed and the size of the arrays for each parameter should be changed 

                    VehFuelCosts(InputCount, 0, 0) = 0.3064 * 26.604
                    'calculation is: ((old el price / old petrol price) * (kwh/100km/l petrol/100km)) * %variable costs) / (%fixed costs + ((old el price / old petrol price) * (kwh/100km/l petrol/100km) * %variable costs)) * total base costs 
                    VehFuelCosts(InputCount, 0, 5) = ((EleOld / PetOld) * (3.2 / 4) * 0.3064) / (0.6936 + ((EleOld / PetOld) * (3.2 / 4) * 0.3064)) * 26.604
                    VehFuelCosts(InputCount, 0, 9) = ((HydOld / PetOld) * (7.6 / 4) * 0.3064) / (0.6936 + ((HydOld / PetOld) * (7.6 / 4) * 0.3064)) * 26.604
                    VehFuelCosts(InputCount, 1, 0) = 0.2337 * 36.14
                    VehFuelCosts(InputCount, 1, 1) = 0.1911 * 36.873
                    VehFuelCosts(InputCount, 1, 2) = ((11.2 / 18.6) * 0.2337) / (0.7663 + ((11.2 / 18.6) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 1, 3) = ((7.6 / 12.4) * 0.1911) / (0.8089 + ((7.6 / 12.4) * 0.1911)) * 36.873
                    'plug in hybrids use petrol on motorways
                    VehFuelCosts(InputCount, 1, 4) = ((18.1 / 25.9) * 0.2337) / (0.7663 + ((18.1 / 25.9) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 1, 5) = ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337) / (0.7663 + ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 1, 8) = ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 1, 9) = ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 2, 0) = 0.155 * 61.329
                    VehFuelCosts(InputCount, 2, 1) = 0.155 * 61.329
                    VehFuelCosts(InputCount, 2, 3) = ((4.4 / 7.9) * 0.155) / (0.845 + ((4.4 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 2, 4) = ((5.8 / 7.9) * 0.155) / (0.845 + ((5.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 2, 5) = ((EleOld / DieOld) * (56.2 / 7.9) * 0.155) / (0.845 + ((EleOld / DieOld) * (56.2 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 2, 6) = ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155) / (0.845 + ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 2, 7) = ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155) / (0.845 + ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 3, 1) = 0.1301 * 234.5
                    VehFuelCosts(InputCount, 3, 3) = ((30.4 / 37.2) * 0.1301) / (0.8699 + ((30.4 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 3, 4) = ((11.9 / 19.6) * 0.1301) / (0.8699 + ((11.9 / 19.6) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 3, 5) = ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301) / (0.8699 + ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 3, 6) = ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301) / (0.8699 + ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 3, 7) = ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301) / (0.8699 + ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 3, 9) = ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301) / (0.8699 + ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 4, 1) = 0.2209 * 93.665
                    VehFuelCosts(InputCount, 4, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 4, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 4, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 5, 1) = 0.2935 * 109.948
                    VehFuelCosts(InputCount, 5, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 5, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 5, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 6, 0) = 0.3064 * 26.604
                    VehFuelCosts(InputCount, 6, 5) = ((EleOld / PetOld) * (3.2 / 4) * 0.3064) / (0.6936 + ((EleOld / PetOld) * (3.2 / 4) * 0.3064)) * 26.604
                    VehFuelCosts(InputCount, 6, 9) = ((HydOld / PetOld) * (7.6 / 4) * 0.3064) / (0.6936 + ((HydOld / PetOld) * (7.6 / 4) * 0.3064)) * 26.604
                    VehFuelCosts(InputCount, 7, 0) = 0.2337 * 36.14
                    VehFuelCosts(InputCount, 7, 1) = 0.1911 * 36.873
                    VehFuelCosts(InputCount, 7, 2) = ((11.2 / 18.6) * 0.2337) / (0.7663 + ((11.2 / 18.6) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 7, 3) = ((7.6 / 12.4) * 0.1911) / (0.8089 + ((7.6 / 12.4) * 0.1911)) * 36.873
                    'plug in hybrids use petrol on dual carriageways
                    VehFuelCosts(InputCount, 7, 4) = ((18.1 / 25.9) * 0.2337) / (0.7663 + ((18.1 / 25.9) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 7, 5) = ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337) / (0.7663 + ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 7, 8) = ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 7, 9) = ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 8, 0) = 0.155 * 61.329
                    VehFuelCosts(InputCount, 8, 1) = 0.155 * 61.329
                    VehFuelCosts(InputCount, 8, 3) = ((4.4 / 7.9) * 0.155) / (0.845 + ((4.4 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 8, 4) = ((5.8 / 7.9) * 0.155) / (0.845 + ((5.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 8, 5) = ((EleOld / DieOld) * (56.2 / 7.9) * 0.155) / (0.845 + ((EleOld / DieOld) * (56.2 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 8, 6) = ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155) / (0.845 + ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 8, 7) = ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155) / (0.845 + ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 9, 1) = 0.1301 * 234.5
                    VehFuelCosts(InputCount, 9, 3) = ((30.4 / 37.2) * 0.1301) / (0.8699 + ((30.4 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 9, 4) = ((11.9 / 19.6) * 0.1301) / (0.8699 + ((11.9 / 19.6) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 9, 5) = ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301) / (0.8699 + ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 9, 6) = ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301) / (0.8699 + ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 9, 7) = ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301) / (0.8699 + ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 9, 9) = ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301) / (0.8699 + ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 10, 1) = 0.2209 * 93.665
                    VehFuelCosts(InputCount, 10, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 10, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 10, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 11, 1) = 0.2935 * 109.948
                    VehFuelCosts(InputCount, 11, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 11, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 11, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 12, 0) = 0.3064 * 26.604
                    VehFuelCosts(InputCount, 12, 5) = ((EleOld / PetOld) * (3.2 / 4) * 0.3064) / (0.6936 + ((EleOld / PetOld) * (3.2 / 4) * 0.3064)) * 26.604
                    VehFuelCosts(InputCount, 12, 9) = ((HydOld / PetOld) * (7.6 / 4) * 0.3064) / (0.6936 + ((HydOld / PetOld) * (7.6 / 4) * 0.3064)) * 26.604
                    VehFuelCosts(InputCount, 13, 0) = 0.2337 * 36.14
                    VehFuelCosts(InputCount, 13, 1) = 0.1911 * 36.873
                    VehFuelCosts(InputCount, 13, 2) = ((11.2 / 18.6) * 0.2337) / (0.7663 + ((11.2 / 18.6) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 13, 3) = ((7.6 / 12.4) * 0.1911) / (0.8089 + ((7.6 / 12.4) * 0.1911)) * 36.873
                    'model assumes for this purpose that plug in hybrids use petrol on single carriageways
                    VehFuelCosts(InputCount, 13, 4) = ((18.1 / 25.9) * 0.2337) / (0.7663 + ((18.1 / 25.9) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 13, 5) = ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337) / (0.7663 + ((EleOld / PetOld) * (16.5 / 7.3) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 13, 8) = ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (43.8 / 10.3) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 13, 9) = ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337) / (0.7663 + ((HydOld / PetOld) * (53.3 / 25.9) * 0.2337)) * 36.14
                    VehFuelCosts(InputCount, 14, 0) = 0.155 * 61.329
                    VehFuelCosts(InputCount, 14, 1) = 0.155 * 61.329
                    VehFuelCosts(InputCount, 14, 3) = ((4.4 / 7.9) * 0.155) / (0.845 + ((4.4 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 14, 4) = ((5.8 / 7.9) * 0.155) / (0.845 + ((5.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 14, 5) = ((EleOld / DieOld) * (56.2 / 7.9) * 0.155) / (0.845 + ((EleOld / DieOld) * (56.2 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 14, 6) = ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155) / (0.845 + ((LPGOld / DieOld) * (11.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 14, 7) = ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155) / (0.845 + ((CNGOld / DieOld) * (80.8 / 7.9) * 0.155)) * 61.329
                    VehFuelCosts(InputCount, 15, 1) = 0.1301 * 234.5
                    VehFuelCosts(InputCount, 15, 3) = ((30.4 / 37.2) * 0.1301) / (0.8699 + ((30.4 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 15, 4) = ((11.9 / 19.6) * 0.1301) / (0.8699 + ((11.9 / 19.6) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 15, 5) = ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301) / (0.8699 + ((EleOld / DieOld) * (425.4 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 15, 6) = ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301) / (0.8699 + ((LPGOld / DieOld) * (131.8 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 15, 7) = ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301) / (0.8699 + ((CNGOld / DieOld) * (1003.2 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 15, 9) = ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301) / (0.8699 + ((HydOld / DieOld) * (109.2 / 37.2) * 0.1301)) * 234.5
                    VehFuelCosts(InputCount, 16, 1) = 0.2209 * 93.665
                    VehFuelCosts(InputCount, 16, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 16, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 16, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 17, 1) = 0.2209 * 93.665
                    VehFuelCosts(InputCount, 17, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 17, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 17, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2209)) * 93.665
                    VehFuelCosts(InputCount, 18, 1) = 0.2935 * 109.948
                    VehFuelCosts(InputCount, 18, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 18, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 18, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 19, 1) = 0.2935 * 109.948
                    VehFuelCosts(InputCount, 19, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 19, 8) = ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (139.8 / 37.6) * 0.2935)) * 109.948
                    VehFuelCosts(InputCount, 19, 9) = ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld / DieOld) * (112.3 / 37.6) * 0.2935)) * 109.948


                End If

                FlowID(InputCount, 1) = InputArray(InputCount, 4)
                OZone(InputCount, 1) = InputArray(InputCount, 5)
                DZone(InputCount, 1) = InputArray(InputCount, 6)
                Pop1Old(InputCount, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, FlowID(InputCount, 1), "OZ", "'road'")
                Pop2Old(InputCount, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, FlowID(InputCount, 1), "DZ", "'road'")
                GVA1Old(InputCount, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, FlowID(InputCount, 1), "OZ", "'road'")
                GVA2Old(InputCount, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, FlowID(InputCount, 1), "DZ", "'road'")

                CostsOld(InputCount, 0) = InputArray(InputCount, 32)
                MLanes(InputCount, 1) = InputArray(InputCount, 8)
                DLanes(InputCount, 1) = InputArray(InputCount, 9)
                SLanes(InputCount, 1) = InputArray(InputCount, 10)
                MCap(InputCount, 1) = InputArray(InputCount, 33)
                DCap(InputCount, 1) = InputArray(InputCount, 34)
                SCap(InputCount, 1) = InputArray(InputCount, 35)
                countvar = 36
                For x = 1 To 19
                    CostsOld(InputCount, x) = CDbl(InputArray(InputCount, countvar))
                    countvar += 1
                Next
                PetOldArr(InputCount, 1) = PetOld
                DieOldArr(InputCount, 1) = DieOld
                EleOldArr(InputCount, 1) = EleOld
                LPGOldArr(InputCount, 1) = LPGOld
                CNGOldArr(InputCount, 1) = CNGOld
                HydOldArr(InputCount, 1) = HydOld

                '030114 change set fuel efficiency old values to one
                For f = 0 To 34
                    FuelEffOld(InputCount, f) = 1
                Next

            Else
                'if not initial year, read data from previous year's result
                'set old values as previous year's values
                FlowID(InputCount, 1) = InputArray(InputCount, 4)
                OZone(InputCount, 1) = InputArray(InputCount, 5)
                DZone(InputCount, 1) = InputArray(InputCount, 6)
                Pop1Old(InputCount, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, FlowID(InputCount, 1), "OZ", "'road'")
                Pop2Old(InputCount, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, FlowID(InputCount, 1), "DZ", "'road'")
                GVA1Old(InputCount, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, FlowID(InputCount, 1), "OZ", "'road'")
                GVA2Old(InputCount, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, FlowID(InputCount, 1), "DZ", "'road'")

                PetOldArr(InputCount, 1) = enearray(g_modelRunYear, 1)
                DieOldArr(InputCount, 1) = enearray(g_modelRunYear, 2)
                EleOldArr(InputCount, 1) = enearray(g_modelRunYear, 3)
                LPGOldArr(InputCount, 1) = enearray(g_modelRunYear, 4)
                CNGOldArr(InputCount, 1) = enearray(g_modelRunYear, 5)
                HydOldArr(InputCount, 1) = enearray(g_modelRunYear, 6)

                For x = 0 To 19
                    CostsOld(InputCount, x) = RdLEVInputArray(InputCount, x + 14)
                Next

                '030114 change
                For f = 0 To 34
                    FuelEffOld(InputCount, f) = stratarray(g_modelRunYear - 2010 - 1, f + 31)
                Next

                'read all fuel cost for all speed categories
                VehFuelCosts(InputCount, 0, 0) = RdLEVInputArray(InputCount, 34)
                VehFuelCosts(InputCount, 0, 5) = RdLEVInputArray(InputCount, 35)
                VehFuelCosts(InputCount, 0, 9) = RdLEVInputArray(InputCount, 36)
                VehFuelCosts(InputCount, 1, 0) = RdLEVInputArray(InputCount, 37)
                VehFuelCosts(InputCount, 1, 1) = RdLEVInputArray(InputCount, 38)
                VehFuelCosts(InputCount, 1, 2) = RdLEVInputArray(InputCount, 39)
                VehFuelCosts(InputCount, 1, 3) = RdLEVInputArray(InputCount, 40)
                VehFuelCosts(InputCount, 1, 4) = RdLEVInputArray(InputCount, 41)
                VehFuelCosts(InputCount, 1, 5) = RdLEVInputArray(InputCount, 42)
                VehFuelCosts(InputCount, 1, 8) = RdLEVInputArray(InputCount, 43)
                VehFuelCosts(InputCount, 1, 9) = RdLEVInputArray(InputCount, 44)
                VehFuelCosts(InputCount, 2, 0) = RdLEVInputArray(InputCount, 45)
                VehFuelCosts(InputCount, 2, 1) = RdLEVInputArray(InputCount, 46)
                VehFuelCosts(InputCount, 2, 3) = RdLEVInputArray(InputCount, 47)
                VehFuelCosts(InputCount, 2, 4) = RdLEVInputArray(InputCount, 48)
                VehFuelCosts(InputCount, 2, 5) = RdLEVInputArray(InputCount, 49)
                VehFuelCosts(InputCount, 2, 6) = RdLEVInputArray(InputCount, 50)
                VehFuelCosts(InputCount, 2, 7) = RdLEVInputArray(InputCount, 51)
                VehFuelCosts(InputCount, 3, 1) = RdLEVInputArray(InputCount, 52)
                VehFuelCosts(InputCount, 3, 3) = RdLEVInputArray(InputCount, 53)
                VehFuelCosts(InputCount, 3, 4) = RdLEVInputArray(InputCount, 54)
                VehFuelCosts(InputCount, 3, 5) = RdLEVInputArray(InputCount, 55)
                VehFuelCosts(InputCount, 3, 6) = RdLEVInputArray(InputCount, 56)
                VehFuelCosts(InputCount, 3, 7) = RdLEVInputArray(InputCount, 57)
                VehFuelCosts(InputCount, 3, 9) = RdLEVInputArray(InputCount, 58)
                VehFuelCosts(InputCount, 4, 1) = RdLEVInputArray(InputCount, 59)
                VehFuelCosts(InputCount, 4, 3) = RdLEVInputArray(InputCount, 60)
                VehFuelCosts(InputCount, 4, 8) = RdLEVInputArray(InputCount, 61)
                VehFuelCosts(InputCount, 4, 9) = RdLEVInputArray(InputCount, 62)
                VehFuelCosts(InputCount, 5, 1) = RdLEVInputArray(InputCount, 63)
                VehFuelCosts(InputCount, 5, 3) = RdLEVInputArray(InputCount, 64)
                VehFuelCosts(InputCount, 5, 8) = RdLEVInputArray(InputCount, 65)
                VehFuelCosts(InputCount, 5, 9) = RdLEVInputArray(InputCount, 66)
                VehFuelCosts(InputCount, 6, 0) = RdLEVInputArray(InputCount, 67)
                VehFuelCosts(InputCount, 6, 5) = RdLEVInputArray(InputCount, 68)
                VehFuelCosts(InputCount, 6, 9) = RdLEVInputArray(InputCount, 69)
                VehFuelCosts(InputCount, 7, 0) = RdLEVInputArray(InputCount, 70)
                VehFuelCosts(InputCount, 7, 1) = RdLEVInputArray(InputCount, 71)
                VehFuelCosts(InputCount, 7, 2) = RdLEVInputArray(InputCount, 72)
                VehFuelCosts(InputCount, 7, 3) = RdLEVInputArray(InputCount, 73)
                VehFuelCosts(InputCount, 7, 4) = RdLEVInputArray(InputCount, 74)
                VehFuelCosts(InputCount, 7, 5) = RdLEVInputArray(InputCount, 75)
                VehFuelCosts(InputCount, 7, 8) = RdLEVInputArray(InputCount, 76)
                VehFuelCosts(InputCount, 7, 9) = RdLEVInputArray(InputCount, 77)
                VehFuelCosts(InputCount, 8, 0) = RdLEVInputArray(InputCount, 78)
                VehFuelCosts(InputCount, 8, 1) = RdLEVInputArray(InputCount, 79)
                VehFuelCosts(InputCount, 8, 3) = RdLEVInputArray(InputCount, 80)
                VehFuelCosts(InputCount, 8, 4) = RdLEVInputArray(InputCount, 81)
                VehFuelCosts(InputCount, 8, 5) = RdLEVInputArray(InputCount, 82)
                VehFuelCosts(InputCount, 8, 6) = RdLEVInputArray(InputCount, 83)
                VehFuelCosts(InputCount, 8, 7) = RdLEVInputArray(InputCount, 84)
                VehFuelCosts(InputCount, 9, 1) = RdLEVInputArray(InputCount, 85)
                VehFuelCosts(InputCount, 9, 3) = RdLEVInputArray(InputCount, 86)
                VehFuelCosts(InputCount, 9, 4) = RdLEVInputArray(InputCount, 87)
                VehFuelCosts(InputCount, 9, 5) = RdLEVInputArray(InputCount, 88)
                VehFuelCosts(InputCount, 9, 6) = RdLEVInputArray(InputCount, 89)
                VehFuelCosts(InputCount, 9, 7) = RdLEVInputArray(InputCount, 90)
                VehFuelCosts(InputCount, 9, 9) = RdLEVInputArray(InputCount, 91)
                VehFuelCosts(InputCount, 10, 1) = RdLEVInputArray(InputCount, 92)
                VehFuelCosts(InputCount, 10, 3) = RdLEVInputArray(InputCount, 93)
                VehFuelCosts(InputCount, 10, 8) = RdLEVInputArray(InputCount, 94)
                VehFuelCosts(InputCount, 10, 9) = RdLEVInputArray(InputCount, 95)
                VehFuelCosts(InputCount, 11, 1) = RdLEVInputArray(InputCount, 96)
                VehFuelCosts(InputCount, 11, 3) = RdLEVInputArray(InputCount, 97)
                VehFuelCosts(InputCount, 11, 8) = RdLEVInputArray(InputCount, 98)
                VehFuelCosts(InputCount, 11, 9) = RdLEVInputArray(InputCount, 99)
                VehFuelCosts(InputCount, 12, 0) = RdLEVInputArray(InputCount, 100)
                VehFuelCosts(InputCount, 12, 5) = RdLEVInputArray(InputCount, 101)
                VehFuelCosts(InputCount, 12, 9) = RdLEVInputArray(InputCount, 102)
                VehFuelCosts(InputCount, 13, 0) = RdLEVInputArray(InputCount, 103)
                VehFuelCosts(InputCount, 13, 1) = RdLEVInputArray(InputCount, 104)
                VehFuelCosts(InputCount, 13, 2) = RdLEVInputArray(InputCount, 105)
                VehFuelCosts(InputCount, 13, 3) = RdLEVInputArray(InputCount, 106)
                VehFuelCosts(InputCount, 13, 4) = RdLEVInputArray(InputCount, 107)
                VehFuelCosts(InputCount, 13, 5) = RdLEVInputArray(InputCount, 108)
                VehFuelCosts(InputCount, 13, 8) = RdLEVInputArray(InputCount, 109)
                VehFuelCosts(InputCount, 13, 9) = RdLEVInputArray(InputCount, 110)
                VehFuelCosts(InputCount, 14, 0) = RdLEVInputArray(InputCount, 111)
                VehFuelCosts(InputCount, 14, 1) = RdLEVInputArray(InputCount, 112)
                VehFuelCosts(InputCount, 14, 3) = RdLEVInputArray(InputCount, 113)
                VehFuelCosts(InputCount, 14, 4) = RdLEVInputArray(InputCount, 114)
                VehFuelCosts(InputCount, 14, 5) = RdLEVInputArray(InputCount, 115)
                VehFuelCosts(InputCount, 14, 6) = RdLEVInputArray(InputCount, 116)
                VehFuelCosts(InputCount, 14, 7) = RdLEVInputArray(InputCount, 117)
                VehFuelCosts(InputCount, 15, 1) = RdLEVInputArray(InputCount, 118)
                VehFuelCosts(InputCount, 15, 3) = RdLEVInputArray(InputCount, 119)
                VehFuelCosts(InputCount, 15, 4) = RdLEVInputArray(InputCount, 120)
                VehFuelCosts(InputCount, 15, 5) = RdLEVInputArray(InputCount, 121)
                VehFuelCosts(InputCount, 15, 6) = RdLEVInputArray(InputCount, 122)
                VehFuelCosts(InputCount, 15, 7) = RdLEVInputArray(InputCount, 123)
                VehFuelCosts(InputCount, 15, 9) = RdLEVInputArray(InputCount, 124)
                VehFuelCosts(InputCount, 16, 1) = RdLEVInputArray(InputCount, 125)
                VehFuelCosts(InputCount, 16, 3) = RdLEVInputArray(InputCount, 126)
                VehFuelCosts(InputCount, 16, 8) = RdLEVInputArray(InputCount, 127)
                VehFuelCosts(InputCount, 16, 9) = RdLEVInputArray(InputCount, 128)
                VehFuelCosts(InputCount, 17, 1) = RdLEVInputArray(InputCount, 129)
                VehFuelCosts(InputCount, 17, 3) = RdLEVInputArray(InputCount, 130)
                VehFuelCosts(InputCount, 17, 8) = RdLEVInputArray(InputCount, 131)
                VehFuelCosts(InputCount, 17, 9) = RdLEVInputArray(InputCount, 132)
                VehFuelCosts(InputCount, 18, 1) = RdLEVInputArray(InputCount, 133)
                VehFuelCosts(InputCount, 18, 3) = RdLEVInputArray(InputCount, 134)
                VehFuelCosts(InputCount, 18, 8) = RdLEVInputArray(InputCount, 135)
                VehFuelCosts(InputCount, 18, 9) = RdLEVInputArray(InputCount, 136)
                VehFuelCosts(InputCount, 19, 1) = RdLEVInputArray(InputCount, 137)
                VehFuelCosts(InputCount, 19, 3) = RdLEVInputArray(InputCount, 138)
                VehFuelCosts(InputCount, 19, 8) = RdLEVInputArray(InputCount, 139)
                VehFuelCosts(InputCount, 19, 9) = RdLEVInputArray(InputCount, 140)

            End If



            If RdLPopSource = "Constant" Then
                Pop1New = Pop1Old(InputCount, 1) * PopGrowth
                Pop2New = Pop2Old(InputCount, 1) * PopGrowth
            ElseIf RdLPopSource = "File" Then
                '***scaling files not currently set up for road links module
            ElseIf RdLPopSource = "Database" Then
                'if year is after 2093 then no population forecasts are available so assume population remains constant
                'now modified as population data available up to 2100 - so should never need 'else'
                'v1.9 now read by using database function
                Pop1New = get_population_data_by_zoneID(g_modelRunYear, FlowID(InputCount, 1), "OZ", "'road'")
                Pop2New = get_population_data_by_zoneID(g_modelRunYear, FlowID(InputCount, 1), "DZ", "'road'")
            End If
            If RdLEcoSource = "Constant" Then
                GVA1New = GVA1Old(InputCount, 1) * GVAGrowth
                GVA2New = GVA2Old(InputCount, 1) * GVAGrowth
            ElseIf RdLEcoSource = "File" Then
                '***scaling files not currently set up for road links module
            ElseIf RdLEcoSource = "Database" Then
                'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                'now modified as gva data available up to 2100 - so should never need 'else'
                'v1.9 now read by using database function
                'database does not have gva forecasts after year 2050, and the calculation is only available before year 2050
                GVA1New = get_gva_data_by_zoneID(g_modelRunYear, FlowID(InputCount, 1), "OZ", "'road'")
                GVA2New = get_gva_data_by_zoneID(g_modelRunYear, FlowID(InputCount, 1), "DZ", "'road'")
            End If
            If RdLEneSource = "Constant" Then
                For x = 0 To 19
                    CostsNew(x) = CostsOld(InputCount, x) * CostGrowth
                Next
            ElseIf RdLEneSource = "File" Then
                'not set up for scaling files
            ElseIf RdLEneSource = "Database" Then
                PetNew = enearray(g_modelRunYear + 1, 1)
                DieNew = enearray(g_modelRunYear + 1, 2)
                EleNew = enearray(g_modelRunYear + 1, 3)
                LPGNew = enearray(g_modelRunYear + 1, 4)
                CNGNew = enearray(g_modelRunYear + 1, 5)
                HydNew = enearray(g_modelRunYear + 1, 6)
                'calculate ratio for each fuel
                PetRat = PetNew / PetOldArr(InputCount, 1)
                DieRat = DieNew / DieOldArr(InputCount, 1)
                EleRat = EleNew / EleOldArr(InputCount, 1)
                LPGRat = LPGNew / LPGOldArr(InputCount, 1)
                CNGRat = CNGNew / CNGOldArr(InputCount, 1)
                HydRat = HydNew / HydOldArr(InputCount, 1)
                '0301014 corrected fuel efficiency change calculation - was previously just multiplying by figure straight from strategy array (which meant that fuel costs quickly declined to zero)
                For f = 0 To 34
                    FuelEffNew(InputCount, f) = stratarray(g_modelRunYear - 2010, f + 31)
                    FuelEffChange(f) = FuelEffNew(InputCount, f) / FuelEffOld(InputCount, f)
                Next
                'calculate cost for each vehicle type - these are the 19 speed categories
                'calculate new cost for each fuel type within each vehicle type - 0 is petrol, 1 is diesel, 2 is petrol hybrid, 3 is diesel hybrid, 4 is plug-in hybrid, 5 is battery electric,
                '...6 is LPG, 7 is CNG, 8 is hydrogen IC, 9 is hydrogen fuel cell - by multiplying the fuel cost by the fuel ratio
                'the cost is also multiplied by changes in fuel efficiency
                VehFuelCosts(InputCount, 0, 0) = VehFuelCosts(InputCount, 0, 0) * PetRat * FuelEffChange(32)
                VehFuelCosts(InputCount, 0, 5) = VehFuelCosts(InputCount, 0, 5) * EleRat * FuelEffChange(33)
                VehFuelCosts(InputCount, 0, 9) = VehFuelCosts(InputCount, 0, 9) * HydRat * FuelEffChange(34)
                VehFuelCosts(InputCount, 1, 0) = VehFuelCosts(InputCount, 1, 0) * PetRat * FuelEffChange(0)
                VehFuelCosts(InputCount, 1, 1) = VehFuelCosts(InputCount, 1, 1) * DieRat * FuelEffChange(1)
                VehFuelCosts(InputCount, 1, 2) = VehFuelCosts(InputCount, 1, 2) * PetRat * FuelEffChange(12)
                VehFuelCosts(InputCount, 1, 3) = VehFuelCosts(InputCount, 1, 3) * DieRat * FuelEffChange(13)
                VehFuelCosts(InputCount, 1, 4) = VehFuelCosts(InputCount, 1, 4) * PetRat * FuelEffChange(14)
                VehFuelCosts(InputCount, 1, 5) = VehFuelCosts(InputCount, 1, 5) * EleRat * FuelEffChange(2)
                VehFuelCosts(InputCount, 1, 8) = VehFuelCosts(InputCount, 1, 8) * HydRat * FuelEffChange(15)
                VehFuelCosts(InputCount, 1, 9) = VehFuelCosts(InputCount, 1, 9) * HydRat * FuelEffChange(16)
                VehFuelCosts(InputCount, 2, 0) = VehFuelCosts(InputCount, 2, 0) * PetRat * FuelEffChange(3)
                VehFuelCosts(InputCount, 2, 1) = VehFuelCosts(InputCount, 2, 1) * PetRat * FuelEffChange(4)
                VehFuelCosts(InputCount, 2, 3) = VehFuelCosts(InputCount, 2, 3) * DieRat * FuelEffChange(17)
                VehFuelCosts(InputCount, 2, 4) = VehFuelCosts(InputCount, 2, 4) * DieRat * FuelEffChange(18)
                VehFuelCosts(InputCount, 2, 5) = VehFuelCosts(InputCount, 2, 5) * EleRat * FuelEffChange(5)
                VehFuelCosts(InputCount, 2, 6) = VehFuelCosts(InputCount, 2, 6) * LPGRat * FuelEffChange(19)
                VehFuelCosts(InputCount, 2, 7) = VehFuelCosts(InputCount, 2, 7) * CNGRat * FuelEffChange(20)
                VehFuelCosts(InputCount, 3, 1) = VehFuelCosts(InputCount, 3, 1) * DieRat * FuelEffChange(10)
                VehFuelCosts(InputCount, 3, 3) = VehFuelCosts(InputCount, 3, 3) * DieRat * FuelEffChange(21)
                VehFuelCosts(InputCount, 3, 4) = VehFuelCosts(InputCount, 3, 4) * DieRat * FuelEffChange(22)
                VehFuelCosts(InputCount, 3, 5) = VehFuelCosts(InputCount, 3, 5) * EleRat * FuelEffChange(11)
                VehFuelCosts(InputCount, 3, 6) = VehFuelCosts(InputCount, 3, 6) * LPGRat * FuelEffChange(23)
                VehFuelCosts(InputCount, 3, 7) = VehFuelCosts(InputCount, 3, 7) * CNGRat * FuelEffChange(24)
                VehFuelCosts(InputCount, 3, 9) = VehFuelCosts(InputCount, 3, 9) * HydRat * FuelEffChange(25)
                VehFuelCosts(InputCount, 4, 1) = VehFuelCosts(InputCount, 4, 1) * DieRat * FuelEffChange(6)
                VehFuelCosts(InputCount, 4, 3) = VehFuelCosts(InputCount, 4, 3) * DieRat * FuelEffChange(26)
                VehFuelCosts(InputCount, 4, 8) = VehFuelCosts(InputCount, 4, 8) * HydRat * FuelEffChange(27)
                VehFuelCosts(InputCount, 4, 9) = VehFuelCosts(InputCount, 4, 9) * HydRat * FuelEffChange(28)
                VehFuelCosts(InputCount, 5, 1) = VehFuelCosts(InputCount, 5, 1) * DieRat * FuelEffChange(8)
                VehFuelCosts(InputCount, 5, 3) = VehFuelCosts(InputCount, 5, 3) * DieRat * FuelEffChange(29)
                VehFuelCosts(InputCount, 5, 8) = VehFuelCosts(InputCount, 5, 8) * HydRat * FuelEffChange(30)
                VehFuelCosts(InputCount, 5, 9) = VehFuelCosts(InputCount, 5, 9) * HydRat * FuelEffChange(31)
                VehFuelCosts(InputCount, 6, 0) = VehFuelCosts(InputCount, 6, 0) * PetRat * FuelEffChange(32)
                VehFuelCosts(InputCount, 6, 5) = VehFuelCosts(InputCount, 6, 5) * EleRat * FuelEffChange(33)
                VehFuelCosts(InputCount, 6, 9) = VehFuelCosts(InputCount, 6, 9) * HydRat * FuelEffChange(34)
                VehFuelCosts(InputCount, 7, 0) = VehFuelCosts(InputCount, 7, 0) * PetRat * FuelEffChange(0)
                VehFuelCosts(InputCount, 7, 1) = VehFuelCosts(InputCount, 7, 1) * DieRat * FuelEffChange(1)
                VehFuelCosts(InputCount, 7, 2) = VehFuelCosts(InputCount, 7, 2) * PetRat * FuelEffChange(12)
                VehFuelCosts(InputCount, 7, 3) = VehFuelCosts(InputCount, 7, 3) * DieRat * FuelEffChange(13)
                VehFuelCosts(InputCount, 7, 4) = VehFuelCosts(InputCount, 7, 4) * PetRat * FuelEffChange(14)
                VehFuelCosts(InputCount, 7, 5) = VehFuelCosts(InputCount, 7, 5) * EleRat * FuelEffChange(2)
                VehFuelCosts(InputCount, 7, 8) = VehFuelCosts(InputCount, 7, 8) * HydRat * FuelEffChange(15)
                VehFuelCosts(InputCount, 7, 9) = VehFuelCosts(InputCount, 7, 9) * HydRat * FuelEffChange(16)
                VehFuelCosts(InputCount, 8, 0) = VehFuelCosts(InputCount, 8, 0) * PetRat * FuelEffChange(3)
                VehFuelCosts(InputCount, 8, 1) = VehFuelCosts(InputCount, 8, 1) * PetRat * FuelEffChange(4)
                VehFuelCosts(InputCount, 8, 3) = VehFuelCosts(InputCount, 8, 3) * DieRat * FuelEffChange(17)
                VehFuelCosts(InputCount, 8, 4) = VehFuelCosts(InputCount, 8, 4) * DieRat * FuelEffChange(18)
                VehFuelCosts(InputCount, 8, 5) = VehFuelCosts(InputCount, 8, 5) * EleRat * FuelEffChange(5)
                VehFuelCosts(InputCount, 8, 6) = VehFuelCosts(InputCount, 8, 6) * LPGRat * FuelEffChange(19)
                VehFuelCosts(InputCount, 8, 7) = VehFuelCosts(InputCount, 8, 7) * CNGRat * FuelEffChange(20)
                VehFuelCosts(InputCount, 9, 1) = VehFuelCosts(InputCount, 9, 1) * DieRat * FuelEffChange(10)
                VehFuelCosts(InputCount, 9, 3) = VehFuelCosts(InputCount, 9, 3) * DieRat * FuelEffChange(21)
                VehFuelCosts(InputCount, 9, 4) = VehFuelCosts(InputCount, 9, 4) * DieRat * FuelEffChange(22)
                VehFuelCosts(InputCount, 9, 5) = VehFuelCosts(InputCount, 9, 5) * EleRat * FuelEffChange(11)
                VehFuelCosts(InputCount, 9, 6) = VehFuelCosts(InputCount, 9, 6) * LPGRat * FuelEffChange(23)
                VehFuelCosts(InputCount, 9, 7) = VehFuelCosts(InputCount, 9, 7) * CNGRat * FuelEffChange(24)
                VehFuelCosts(InputCount, 9, 9) = VehFuelCosts(InputCount, 9, 9) * HydRat * FuelEffChange(25)
                VehFuelCosts(InputCount, 10, 1) = VehFuelCosts(InputCount, 10, 1) * DieRat * FuelEffChange(6)
                VehFuelCosts(InputCount, 10, 3) = VehFuelCosts(InputCount, 10, 3) * DieRat * FuelEffChange(26)
                VehFuelCosts(InputCount, 10, 8) = VehFuelCosts(InputCount, 10, 8) * HydRat * FuelEffChange(27)
                VehFuelCosts(InputCount, 10, 9) = VehFuelCosts(InputCount, 10, 9) * HydRat * FuelEffChange(28)
                VehFuelCosts(InputCount, 11, 1) = VehFuelCosts(InputCount, 11, 1) * DieRat * FuelEffChange(8)
                VehFuelCosts(InputCount, 11, 3) = VehFuelCosts(InputCount, 11, 3) * DieRat * FuelEffChange(29)
                VehFuelCosts(InputCount, 11, 8) = VehFuelCosts(InputCount, 11, 8) * HydRat * FuelEffChange(30)
                VehFuelCosts(InputCount, 11, 9) = VehFuelCosts(InputCount, 11, 9) * HydRat * FuelEffChange(31)
                VehFuelCosts(InputCount, 12, 0) = VehFuelCosts(InputCount, 12, 0) * PetRat * FuelEffChange(32)
                VehFuelCosts(InputCount, 12, 5) = VehFuelCosts(InputCount, 12, 5) * EleRat * FuelEffChange(33)
                VehFuelCosts(InputCount, 12, 9) = VehFuelCosts(InputCount, 12, 9) * HydRat * FuelEffChange(34)
                VehFuelCosts(InputCount, 13, 0) = VehFuelCosts(InputCount, 13, 0) * PetRat * FuelEffChange(0)
                VehFuelCosts(InputCount, 13, 1) = VehFuelCosts(InputCount, 13, 1) * DieRat * FuelEffChange(1)
                VehFuelCosts(InputCount, 13, 2) = VehFuelCosts(InputCount, 13, 2) * PetRat * FuelEffChange(12)
                VehFuelCosts(InputCount, 13, 3) = VehFuelCosts(InputCount, 13, 3) * DieRat * FuelEffChange(13)
                VehFuelCosts(InputCount, 13, 4) = VehFuelCosts(InputCount, 13, 4) * PetRat * FuelEffChange(14)
                VehFuelCosts(InputCount, 13, 5) = VehFuelCosts(InputCount, 13, 5) * EleRat * FuelEffChange(2)
                VehFuelCosts(InputCount, 13, 8) = VehFuelCosts(InputCount, 13, 8) * HydRat * FuelEffChange(15)
                VehFuelCosts(InputCount, 13, 9) = VehFuelCosts(InputCount, 13, 9) * HydRat * FuelEffChange(16)
                VehFuelCosts(InputCount, 14, 0) = VehFuelCosts(InputCount, 14, 0) * PetRat * FuelEffChange(3)
                VehFuelCosts(InputCount, 14, 1) = VehFuelCosts(InputCount, 14, 1) * PetRat * FuelEffChange(4)
                VehFuelCosts(InputCount, 14, 3) = VehFuelCosts(InputCount, 14, 3) * DieRat * FuelEffChange(17)
                VehFuelCosts(InputCount, 14, 4) = VehFuelCosts(InputCount, 14, 4) * DieRat * FuelEffChange(18)
                VehFuelCosts(InputCount, 14, 5) = VehFuelCosts(InputCount, 14, 5) * EleRat * FuelEffChange(5)
                VehFuelCosts(InputCount, 14, 6) = VehFuelCosts(InputCount, 14, 6) * LPGRat * FuelEffChange(19)
                VehFuelCosts(InputCount, 14, 7) = VehFuelCosts(InputCount, 14, 7) * CNGRat * FuelEffChange(20)
                VehFuelCosts(InputCount, 15, 1) = VehFuelCosts(InputCount, 15, 1) * DieRat * FuelEffChange(10)
                VehFuelCosts(InputCount, 15, 3) = VehFuelCosts(InputCount, 15, 3) * DieRat * FuelEffChange(21)
                VehFuelCosts(InputCount, 15, 4) = VehFuelCosts(InputCount, 15, 4) * DieRat * FuelEffChange(22)
                VehFuelCosts(InputCount, 15, 5) = VehFuelCosts(InputCount, 15, 5) * EleRat * FuelEffChange(11)
                VehFuelCosts(InputCount, 15, 6) = VehFuelCosts(InputCount, 15, 6) * LPGRat * FuelEffChange(23)
                VehFuelCosts(InputCount, 15, 7) = VehFuelCosts(InputCount, 15, 7) * CNGRat * FuelEffChange(24)
                VehFuelCosts(InputCount, 15, 9) = VehFuelCosts(InputCount, 15, 9) * HydRat * FuelEffChange(25)
                VehFuelCosts(InputCount, 16, 1) = VehFuelCosts(InputCount, 16, 1) * DieRat * FuelEffChange(6)
                VehFuelCosts(InputCount, 16, 3) = VehFuelCosts(InputCount, 16, 3) * DieRat * FuelEffChange(26)
                VehFuelCosts(InputCount, 16, 8) = VehFuelCosts(InputCount, 16, 8) * HydRat * FuelEffChange(27)
                VehFuelCosts(InputCount, 16, 9) = VehFuelCosts(InputCount, 16, 9) * HydRat * FuelEffChange(28)
                VehFuelCosts(InputCount, 17, 1) = VehFuelCosts(InputCount, 17, 1) * DieRat * FuelEffChange(6)
                VehFuelCosts(InputCount, 17, 3) = VehFuelCosts(InputCount, 17, 3) * DieRat * FuelEffChange(26)
                VehFuelCosts(InputCount, 17, 8) = VehFuelCosts(InputCount, 17, 8) * HydRat * FuelEffChange(27)
                VehFuelCosts(InputCount, 17, 9) = VehFuelCosts(InputCount, 17, 9) * HydRat * FuelEffChange(28)
                VehFuelCosts(InputCount, 18, 1) = VehFuelCosts(InputCount, 18, 1) * DieRat * FuelEffChange(8)
                VehFuelCosts(InputCount, 18, 3) = VehFuelCosts(InputCount, 18, 3) * DieRat * FuelEffChange(29)
                VehFuelCosts(InputCount, 18, 8) = VehFuelCosts(InputCount, 18, 8) * HydRat * FuelEffChange(30)
                VehFuelCosts(InputCount, 18, 9) = VehFuelCosts(InputCount, 18, 9) * HydRat * FuelEffChange(31)
                VehFuelCosts(InputCount, 19, 1) = VehFuelCosts(InputCount, 19, 1) * DieRat * FuelEffChange(8)
                VehFuelCosts(InputCount, 19, 3) = VehFuelCosts(InputCount, 19, 3) * DieRat * FuelEffChange(29)
                VehFuelCosts(InputCount, 19, 8) = VehFuelCosts(InputCount, 19, 8) * HydRat * FuelEffChange(30)
                VehFuelCosts(InputCount, 19, 9) = VehFuelCosts(InputCount, 19, 9) * HydRat * FuelEffChange(31)
                'v1.4 if using carbon charge then need to add that, assuming it is after the year of introduction
                If CarbonCharge = True Then
                    If g_modelRunYear >= CarbChargeYear Then
                        'note that we assume base (2010) petrol price of 122.1 p/litre when calculating the base fuel consumption (full calculations from base figures not included in model run)
                        'calculation is: (base fuel units per km * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
                        CarbCharge(0, 0) = (0.04 * stratarray(g_modelRunYear - 2010, 63) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(0, 5) = (0.032 * stratarray(g_modelRunYear - 2010, 64) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(0, 9) = (0.123 * stratarray(g_modelRunYear - 2010, 65) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(1, 0) = (0.086 * stratarray(g_modelRunYear - 2010, 31) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(1, 1) = (0.057 * stratarray(g_modelRunYear - 2010, 32) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(1, 2) = (0.056 * stratarray(g_modelRunYear - 2010, 43) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(1, 3) = (0.038 * stratarray(g_modelRunYear - 2010, 44) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(1, 4) = (0.06 * stratarray(g_modelRunYear - 2010, 45) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(1, 5) = (0.165 * stratarray(g_modelRunYear - 2010, 33) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(1, 8) = (0.438 * stratarray(g_modelRunYear - 2010, 46) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(1, 9) = (0.178 * stratarray(g_modelRunYear - 2010, 47) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(2, 0) = (0.088 * stratarray(g_modelRunYear - 2010, 34) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(2, 1) = (0.079 * stratarray(g_modelRunYear - 2010, 35) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(2, 3) = (0.044 * stratarray(g_modelRunYear - 2010, 48) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(2, 4) = (0.058 * stratarray(g_modelRunYear - 2010, 49) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(2, 5) = (0.562 * stratarray(g_modelRunYear - 2010, 36) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(2, 6) = (0.118 * stratarray(g_modelRunYear - 2010, 50) * stratarray(g_modelRunYear - 2010, 75) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(2, 7) = (0.808 * stratarray(g_modelRunYear - 2010, 51) * stratarray(g_modelRunYear - 2010, 76) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(3, 1) = (0.176 * stratarray(g_modelRunYear - 2010, 41) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(3, 3) = (0.185 * stratarray(g_modelRunYear - 2010, 52) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(3, 4) = (0.119 * stratarray(g_modelRunYear - 2010, 53) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(3, 5) = (0.2554 * stratarray(g_modelRunYear - 2010, 42) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(3, 6) = (0.954 * stratarray(g_modelRunYear - 2010, 54) * stratarray(g_modelRunYear - 2010, 75) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(3, 7) = (3.749 * stratarray(g_modelRunYear - 2010, 55) * stratarray(g_modelRunYear - 2010, 76) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(3, 9) = (0.546 * stratarray(g_modelRunYear - 2010, 56) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(4, 1) = (0.259 * stratarray(g_modelRunYear - 2010, 37) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(4, 3) = (0.15 * stratarray(g_modelRunYear - 2010, 57) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(4, 8) = (0.957 * stratarray(g_modelRunYear - 2010, 58) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(4, 9) = (0.898 * stratarray(g_modelRunYear - 2010, 59) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(5, 1) = (0.376 * stratarray(g_modelRunYear - 2010, 39) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(5, 3) = (0.221 * stratarray(g_modelRunYear - 2010, 60) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(5, 8) = (1.398 * stratarray(g_modelRunYear - 2010, 61) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(5, 9) = (1.123 * stratarray(g_modelRunYear - 2010, 62) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(6, 0) = (0.04 * stratarray(g_modelRunYear - 2010, 63) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(6, 5) = (0.032 * stratarray(g_modelRunYear - 2010, 64) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(6, 9) = (0.123 * stratarray(g_modelRunYear - 2010, 65) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(7, 0) = (0.086 * stratarray(g_modelRunYear - 2010, 31) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(7, 1) = (0.057 * stratarray(g_modelRunYear - 2010, 32) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(7, 2) = (0.056 * stratarray(g_modelRunYear - 2010, 43) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(7, 3) = (0.038 * stratarray(g_modelRunYear - 2010, 44) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(7, 4) = (0.06 * stratarray(g_modelRunYear - 2010, 45) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(7, 5) = (0.165 * stratarray(g_modelRunYear - 2010, 33) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(7, 8) = (0.438 * stratarray(g_modelRunYear - 2010, 46) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(7, 9) = (0.178 * stratarray(g_modelRunYear - 2010, 47) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(8, 0) = (0.088 * stratarray(g_modelRunYear - 2010, 34) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(8, 1) = (0.079 * stratarray(g_modelRunYear - 2010, 35) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(8, 3) = (0.044 * stratarray(g_modelRunYear - 2010, 48) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(8, 4) = (0.058 * stratarray(g_modelRunYear - 2010, 49) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(8, 5) = (0.562 * stratarray(g_modelRunYear - 2010, 36) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(8, 6) = (0.118 * stratarray(g_modelRunYear - 2010, 50) * stratarray(g_modelRunYear - 2010, 75) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(8, 7) = (0.808 * stratarray(g_modelRunYear - 2010, 51) * stratarray(g_modelRunYear - 2010, 76) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(9, 1) = (0.176 * stratarray(g_modelRunYear - 2010, 41) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(9, 3) = (0.185 * stratarray(g_modelRunYear - 2010, 52) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(9, 4) = (0.119 * stratarray(g_modelRunYear - 2010, 53) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(9, 5) = (2.554 * stratarray(g_modelRunYear - 2010, 42) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(9, 6) = (0.954 * stratarray(g_modelRunYear - 2010, 54) * stratarray(g_modelRunYear - 2010, 75) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(9, 7) = (3.749 * stratarray(g_modelRunYear - 2010, 55) * stratarray(g_modelRunYear - 2010, 76) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(9, 9) = (0.546 * stratarray(g_modelRunYear - 2010, 56) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(10, 1) = (0.259 * stratarray(g_modelRunYear - 2010, 37) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(10, 3) = (0.15 * stratarray(g_modelRunYear - 2010, 57) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(10, 8) = (0.957 * stratarray(g_modelRunYear - 2010, 58) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(10, 9) = (0.898 * stratarray(g_modelRunYear - 2010, 59) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(11, 1) = (0.376 * stratarray(g_modelRunYear - 2010, 39) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(11, 3) = (0.221 * stratarray(g_modelRunYear - 2010, 60) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(11, 8) = (1.398 * stratarray(g_modelRunYear - 2010, 61) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(11, 9) = (1.123 * stratarray(g_modelRunYear - 2010, 62) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(12, 0) = (0.04 * stratarray(g_modelRunYear - 2010, 63) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(12, 5) = (0.032 * stratarray(g_modelRunYear - 2010, 64) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(12, 9) = (0.123 * stratarray(g_modelRunYear - 2010, 65) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(13, 0) = (0.086 * stratarray(g_modelRunYear - 2010, 31) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(13, 1) = (0.057 * stratarray(g_modelRunYear - 2010, 32) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(13, 2) = (0.056 * stratarray(g_modelRunYear - 2010, 43) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(13, 3) = (0.038 * stratarray(g_modelRunYear - 2010, 44) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(13, 4) = (0.016 * stratarray(g_modelRunYear - 2010, 45) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(13, 5) = (0.165 * stratarray(g_modelRunYear - 2010, 33) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(13, 8) = (0.438 * stratarray(g_modelRunYear - 2010, 46) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(13, 9) = (0.178 * stratarray(g_modelRunYear - 2010, 47) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(14, 0) = (0.088 * stratarray(g_modelRunYear - 2010, 34) * stratarray(g_modelRunYear - 2010, 72) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(14, 1) = (0.079 * stratarray(g_modelRunYear - 2010, 35) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(14, 3) = (0.044 * stratarray(g_modelRunYear - 2010, 48) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(14, 4) = (0.423 * stratarray(g_modelRunYear - 2010, 49) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(14, 5) = (0.562 * stratarray(g_modelRunYear - 2010, 36) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(14, 6) = (0.118 * stratarray(g_modelRunYear - 2010, 50) * stratarray(g_modelRunYear - 2010, 75) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(14, 7) = (0.808 * stratarray(g_modelRunYear - 2010, 51) * stratarray(g_modelRunYear - 2010, 76) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(15, 1) = (0.196 * stratarray(g_modelRunYear - 2010, 41) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(15, 3) = (0.119 * stratarray(g_modelRunYear - 2010, 52) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(15, 4) = (1.037 * stratarray(g_modelRunYear - 2010, 53) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(15, 5) = (1.7 * stratarray(g_modelRunYear - 2010, 42) * stratarray(g_modelRunYear - 2010, 74) * (stratarray(g_modelRunYear - 2010, 70) / 10))
                        CarbCharge(15, 6) = (0.364 * stratarray(g_modelRunYear - 2010, 54) * stratarray(g_modelRunYear - 2010, 75) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(15, 7) = (6.283 * stratarray(g_modelRunYear - 2010, 55) * stratarray(g_modelRunYear - 2010, 76) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(15, 9) = (0.546 * stratarray(g_modelRunYear - 2010, 56) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(16, 1) = (0.259 * stratarray(g_modelRunYear - 2010, 37) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(16, 3) = (0.15 * stratarray(g_modelRunYear - 2010, 57) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(16, 8) = (0.957 * stratarray(g_modelRunYear - 2010, 58) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(16, 9) = (0.898 * stratarray(g_modelRunYear - 2010, 59) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(17, 1) = (0.259 * stratarray(g_modelRunYear - 2010, 37) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(17, 3) = (0.15 * stratarray(g_modelRunYear - 2010, 57) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(17, 8) = (0.957 * stratarray(g_modelRunYear - 2010, 58) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(17, 9) = (0.898 * stratarray(g_modelRunYear - 2010, 59) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(18, 1) = (0.376 * stratarray(g_modelRunYear - 2010, 39) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(18, 3) = (0.221 * stratarray(g_modelRunYear - 2010, 60) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(18, 8) = (1.398 * stratarray(g_modelRunYear - 2010, 61) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(18, 9) = (1.123 * stratarray(g_modelRunYear - 2010, 62) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(19, 1) = (0.376 * stratarray(g_modelRunYear - 2010, 39) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(19, 3) = (0.221 * stratarray(g_modelRunYear - 2010, 60) * stratarray(g_modelRunYear - 2010, 73) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(19, 8) = (1.398 * stratarray(g_modelRunYear - 2010, 61) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                        CarbCharge(19, 9) = (1.123 * stratarray(g_modelRunYear - 2010, 62) * stratarray(g_modelRunYear - 2010, 77) * (stratarray(g_modelRunYear - 2010, 71) / 10))
                    End If
                End If

                'add the fixed costs
                'v1.4 and also add the carbon charge if we are using one
                If CarbonCharge = True Then
                    For x = 0 To 19
                        For y = 0 To 9
                            VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(InputCount, x, y) + CarbCharge(x, y)
                        Next
                    Next
                Else
                    For x = 0 To 19
                        For y = 0 To 9
                            VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(InputCount, x, y)
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
                'v1.6 CostNew does not need to increase dimension, as it is the output for each step
                CostsNew(0) = (VehCosts(0, 0) * stratarray(g_modelRunYear - 2010, 11)) + (VehCosts(0, 5) * stratarray(g_modelRunYear - 2010, 12)) + (VehCosts(0, 9) * stratarray(g_modelRunYear - 2010, 13))
                CostsNew(1) = (VehCosts(1, 0) * stratarray(g_modelRunYear - 2010, 1)) + (VehCosts(1, 1) * stratarray(g_modelRunYear - 2010, 2)) + (VehCosts(1, 2) * stratarray(g_modelRunYear - 2010, 14)) + (VehCosts(1, 3) * stratarray(g_modelRunYear - 2010, 15)) + (VehCosts(1, 4) * stratarray(g_modelRunYear - 2010, 16)) + (VehCosts(1, 5) * stratarray(g_modelRunYear - 2010, 3)) + (VehCosts(1, 8) * stratarray(g_modelRunYear - 2010, 17)) + (VehCosts(1, 9) * stratarray(g_modelRunYear - 2010, 18))
                CostsNew(2) = (VehCosts(2, 0) * stratarray(g_modelRunYear - 2010, 4)) + (VehCosts(2, 1) * stratarray(g_modelRunYear - 2010, 5)) + (VehCosts(2, 3) * stratarray(g_modelRunYear - 2010, 19)) + (VehCosts(2, 4) * stratarray(g_modelRunYear - 2010, 20)) + (VehCosts(2, 5) * stratarray(g_modelRunYear - 2010, 6)) + (VehCosts(2, 6) * stratarray(g_modelRunYear - 2010, 21)) + (VehCosts(2, 7) * stratarray(g_modelRunYear - 2010, 22))
                CostsNew(3) = (VehCosts(3, 1) * stratarray(g_modelRunYear - 2010, 9)) + (VehCosts(3, 3) * stratarray(g_modelRunYear - 2010, 23)) + (VehCosts(3, 4) * stratarray(g_modelRunYear - 2010, 24)) + (VehCosts(3, 5) * stratarray(g_modelRunYear - 2010, 10)) + (VehCosts(3, 6) * stratarray(g_modelRunYear - 2010, 25)) + (VehCosts(3, 7) * stratarray(g_modelRunYear - 2010, 26)) + (VehCosts(3, 9) * stratarray(g_modelRunYear - 2010, 27))
                CostsNew(4) = (VehCosts(4, 1) * stratarray(g_modelRunYear - 2010, 7)) + (VehCosts(4, 3) * stratarray(g_modelRunYear - 2010, 28)) + (VehCosts(4, 8) * stratarray(g_modelRunYear - 2010, 29)) + (VehCosts(4, 9) * stratarray(g_modelRunYear - 2010, 30))
                CostsNew(5) = (VehCosts(5, 1) * stratarray(g_modelRunYear - 2010, 7)) + (VehCosts(5, 3) * stratarray(g_modelRunYear - 2010, 28)) + (VehCosts(5, 8) * stratarray(g_modelRunYear - 2010, 29)) + (VehCosts(5, 9) * stratarray(g_modelRunYear - 2010, 30))
                CostsNew(6) = (VehCosts(6, 0) * stratarray(g_modelRunYear - 2010, 11)) + (VehCosts(6, 5) * stratarray(g_modelRunYear - 2010, 12)) + (VehCosts(6, 9) * stratarray(g_modelRunYear - 2010, 13))
                CostsNew(7) = (VehCosts(7, 0) * stratarray(g_modelRunYear - 2010, 1)) + (VehCosts(7, 1) * stratarray(g_modelRunYear - 2010, 2)) + (VehCosts(7, 2) * stratarray(g_modelRunYear - 2010, 14)) + (VehCosts(7, 3) * stratarray(g_modelRunYear - 2010, 15)) + (VehCosts(7, 4) * stratarray(g_modelRunYear - 2010, 16)) + (VehCosts(7, 5) * stratarray(g_modelRunYear - 2010, 3)) + (VehCosts(7, 8) * stratarray(g_modelRunYear - 2010, 17)) + (VehCosts(7, 9) * stratarray(g_modelRunYear - 2010, 18))
                CostsNew(8) = (VehCosts(8, 0) * stratarray(g_modelRunYear - 2010, 4)) + (VehCosts(8, 1) * stratarray(g_modelRunYear - 2010, 5)) + (VehCosts(8, 3) * stratarray(g_modelRunYear - 2010, 19)) + (VehCosts(8, 4) * stratarray(g_modelRunYear - 2010, 20)) + (VehCosts(8, 5) * stratarray(g_modelRunYear - 2010, 6)) + (VehCosts(8, 6) * stratarray(g_modelRunYear - 2010, 21)) + (VehCosts(8, 7) * stratarray(g_modelRunYear - 2010, 22))
                CostsNew(9) = (VehCosts(9, 1) * stratarray(g_modelRunYear - 2010, 9)) + (VehCosts(9, 3) * stratarray(g_modelRunYear - 2010, 23)) + (VehCosts(9, 4) * stratarray(g_modelRunYear - 2010, 24)) + (VehCosts(9, 5) * stratarray(g_modelRunYear - 2010, 10)) + (VehCosts(9, 6) * stratarray(g_modelRunYear - 2010, 25)) + (VehCosts(9, 7) * stratarray(g_modelRunYear - 2010, 26)) + (VehCosts(9, 9) * stratarray(g_modelRunYear - 2010, 27))
                CostsNew(10) = (VehCosts(10, 1) * stratarray(g_modelRunYear - 2010, 7)) + (VehCosts(10, 3) * stratarray(g_modelRunYear - 2010, 28)) + (VehCosts(10, 8) * stratarray(g_modelRunYear - 2010, 29)) + (VehCosts(10, 9) * stratarray(g_modelRunYear - 2010, 30))
                CostsNew(11) = (VehCosts(11, 1) * stratarray(g_modelRunYear - 2010, 7)) + (VehCosts(11, 3) * stratarray(g_modelRunYear - 2010, 28)) + (VehCosts(11, 8) * stratarray(g_modelRunYear - 2010, 29)) + (VehCosts(11, 9) * stratarray(g_modelRunYear - 2010, 30))
                CostsNew(12) = (VehCosts(12, 0) * stratarray(g_modelRunYear - 2010, 11)) + (VehCosts(12, 5) * stratarray(g_modelRunYear - 2010, 12)) + (VehCosts(12, 9) * stratarray(g_modelRunYear - 2010, 13))
                CostsNew(13) = (VehCosts(13, 0) * stratarray(g_modelRunYear - 2010, 1)) + (VehCosts(13, 1) * stratarray(g_modelRunYear - 2010, 2)) + (VehCosts(13, 2) * stratarray(g_modelRunYear - 2010, 14)) + (VehCosts(13, 3) * stratarray(g_modelRunYear - 2010, 15)) + (VehCosts(13, 4) * stratarray(g_modelRunYear - 2010, 16)) + (VehCosts(13, 5) * stratarray(g_modelRunYear - 2010, 3)) + (VehCosts(13, 8) * stratarray(g_modelRunYear - 2010, 17)) + (VehCosts(13, 9) * stratarray(g_modelRunYear - 2010, 18))
                CostsNew(14) = (VehCosts(14, 0) * stratarray(g_modelRunYear - 2010, 4)) + (VehCosts(14, 1) * stratarray(g_modelRunYear - 2010, 5)) + (VehCosts(14, 3) * stratarray(g_modelRunYear - 2010, 19)) + (VehCosts(14, 4) * stratarray(g_modelRunYear - 2010, 20)) + (VehCosts(14, 5) * stratarray(g_modelRunYear - 2010, 6)) + (VehCosts(14, 6) * stratarray(g_modelRunYear - 2010, 21)) + (VehCosts(14, 7) * stratarray(g_modelRunYear - 2010, 22))
                CostsNew(15) = (VehCosts(15, 1) * stratarray(g_modelRunYear - 2010, 9)) + (VehCosts(15, 3) * stratarray(g_modelRunYear - 2010, 23)) + (VehCosts(15, 4) * stratarray(g_modelRunYear - 2010, 24)) + (VehCosts(15, 5) * stratarray(g_modelRunYear - 2010, 10)) + (VehCosts(15, 6) * stratarray(g_modelRunYear - 2010, 25)) + (VehCosts(15, 7) * stratarray(g_modelRunYear - 2010, 26)) + (VehCosts(15, 9) * stratarray(g_modelRunYear - 2010, 27))
                CostsNew(16) = (VehCosts(16, 1) * stratarray(g_modelRunYear - 2010, 7)) + (VehCosts(16, 3) * stratarray(g_modelRunYear - 2010, 28)) + (VehCosts(16, 8) * stratarray(g_modelRunYear - 2010, 29)) + (VehCosts(16, 9) * stratarray(g_modelRunYear - 2010, 30))
                CostsNew(17) = (VehCosts(17, 1) * stratarray(g_modelRunYear - 2010, 7)) + (VehCosts(17, 3) * stratarray(g_modelRunYear - 2010, 28)) + (VehCosts(17, 8) * stratarray(g_modelRunYear - 2010, 29)) + (VehCosts(17, 9) * stratarray(g_modelRunYear - 2010, 30))
                CostsNew(18) = (VehCosts(18, 1) * stratarray(g_modelRunYear - 2010, 7)) + (VehCosts(18, 3) * stratarray(g_modelRunYear - 2010, 28)) + (VehCosts(18, 8) * stratarray(g_modelRunYear - 2010, 29)) + (VehCosts(18, 9) * stratarray(g_modelRunYear - 2010, 30))
                CostsNew(19) = (VehCosts(19, 1) * stratarray(g_modelRunYear - 2010, 7)) + (VehCosts(19, 3) * stratarray(g_modelRunYear - 2010, 28)) + (VehCosts(19, 8) * stratarray(g_modelRunYear - 2010, 29)) + (VehCosts(19, 9) * stratarray(g_modelRunYear - 2010, 30))
            End If

            'if including capacity changes, then check if there are any capacity changes on this flow
            'v1.4 changed to include compulsory capacity changes where construction has already begun
            'all this involves is removing the if newrdlcap = true clause, because this was already accounted for when generating the intermediate file, and adding a lineread above getcapdata because this sub was amended
            If FlowID(InputCount, 1) = CapID Then
                'if there are any capacity changes on this flow, check if there are any capacity changes in this year
                If g_modelRunYear = CapYear Then
                    'if there are, then update the capacity variables, and read in the next row from the capacity file
                    MLanes(InputCount, 1) += MLaneChange
                    DLanes(InputCount, 1) += DLaneChange
                    SLanes(InputCount, 1) += SLaneChange

                    Call GetCapData()
                End If
            End If

            'v1.4 now updates maximum lane capacities from common variables file
            MCap(InputCount, 1) = stratarray(g_modelRunYear - 2010, 79)
            DCap(InputCount, 1) = stratarray(g_modelRunYear - 2010, 80)
            SCap(InputCount, 1) = stratarray(g_modelRunYear - 2010, 81)



            'write to output file
            OutputArray(InputCount, 0) = g_modelRunID
            OutputArray(InputCount, 1) = FlowID(InputCount, 1)
            OutputArray(InputCount, 2) = g_modelRunYear
            OutputArray(InputCount, 3) = Pop1New
            OutputArray(InputCount, 4) = Pop2New
            OutputArray(InputCount, 5) = GVA1New
            OutputArray(InputCount, 6) = GVA2New
            OutputArray(InputCount, 7) = MLanes(InputCount, 1)
            OutputArray(InputCount, 8) = DLanes(InputCount, 1)
            OutputArray(InputCount, 9) = SLanes(InputCount, 1)
            OutputArray(InputCount, 10) = MCap(InputCount, 1)
            OutputArray(InputCount, 11) = DCap(InputCount, 1)
            OutputArray(InputCount, 12) = SCap(InputCount, 1)
            For x = 0 To 19
                OutputArray(InputCount, 13 + x) = CostsNew(x)
            Next

            'write all speed category costs to database
            OutputArray(InputCount, 33) = VehFuelCosts(InputCount, 0, 0)
            OutputArray(InputCount, 34) = VehFuelCosts(InputCount, 0, 5)
            OutputArray(InputCount, 35) = VehFuelCosts(InputCount, 0, 9)
            OutputArray(InputCount, 36) = VehFuelCosts(InputCount, 1, 0)
            OutputArray(InputCount, 37) = VehFuelCosts(InputCount, 1, 1)
            OutputArray(InputCount, 38) = VehFuelCosts(InputCount, 1, 2)
            OutputArray(InputCount, 39) = VehFuelCosts(InputCount, 1, 3)
            OutputArray(InputCount, 40) = VehFuelCosts(InputCount, 1, 4)
            OutputArray(InputCount, 41) = VehFuelCosts(InputCount, 1, 5)
            OutputArray(InputCount, 42) = VehFuelCosts(InputCount, 1, 8)
            OutputArray(InputCount, 43) = VehFuelCosts(InputCount, 1, 9)
            OutputArray(InputCount, 44) = VehFuelCosts(InputCount, 2, 0)
            OutputArray(InputCount, 45) = VehFuelCosts(InputCount, 2, 1)
            OutputArray(InputCount, 46) = VehFuelCosts(InputCount, 2, 3)
            OutputArray(InputCount, 47) = VehFuelCosts(InputCount, 2, 4)
            OutputArray(InputCount, 48) = VehFuelCosts(InputCount, 2, 5)
            OutputArray(InputCount, 49) = VehFuelCosts(InputCount, 2, 6)
            OutputArray(InputCount, 50) = VehFuelCosts(InputCount, 2, 7)
            OutputArray(InputCount, 51) = VehFuelCosts(InputCount, 3, 1)
            OutputArray(InputCount, 52) = VehFuelCosts(InputCount, 3, 3)
            OutputArray(InputCount, 53) = VehFuelCosts(InputCount, 3, 4)
            OutputArray(InputCount, 54) = VehFuelCosts(InputCount, 3, 5)
            OutputArray(InputCount, 55) = VehFuelCosts(InputCount, 3, 6)
            OutputArray(InputCount, 56) = VehFuelCosts(InputCount, 3, 7)
            OutputArray(InputCount, 57) = VehFuelCosts(InputCount, 3, 9)
            OutputArray(InputCount, 58) = VehFuelCosts(InputCount, 4, 1)
            OutputArray(InputCount, 59) = VehFuelCosts(InputCount, 4, 3)
            OutputArray(InputCount, 60) = VehFuelCosts(InputCount, 4, 8)
            OutputArray(InputCount, 61) = VehFuelCosts(InputCount, 4, 9)
            OutputArray(InputCount, 62) = VehFuelCosts(InputCount, 5, 1)
            OutputArray(InputCount, 63) = VehFuelCosts(InputCount, 5, 3)
            OutputArray(InputCount, 64) = VehFuelCosts(InputCount, 5, 8)
            OutputArray(InputCount, 65) = VehFuelCosts(InputCount, 5, 9)
            OutputArray(InputCount, 66) = VehFuelCosts(InputCount, 6, 0)
            OutputArray(InputCount, 67) = VehFuelCosts(InputCount, 6, 5)
            OutputArray(InputCount, 68) = VehFuelCosts(InputCount, 6, 9)
            OutputArray(InputCount, 69) = VehFuelCosts(InputCount, 7, 0)
            OutputArray(InputCount, 70) = VehFuelCosts(InputCount, 7, 1)
            OutputArray(InputCount, 71) = VehFuelCosts(InputCount, 7, 2)
            OutputArray(InputCount, 72) = VehFuelCosts(InputCount, 7, 3)
            OutputArray(InputCount, 73) = VehFuelCosts(InputCount, 7, 4)
            OutputArray(InputCount, 74) = VehFuelCosts(InputCount, 7, 5)
            OutputArray(InputCount, 75) = VehFuelCosts(InputCount, 7, 8)
            OutputArray(InputCount, 76) = VehFuelCosts(InputCount, 7, 9)
            OutputArray(InputCount, 77) = VehFuelCosts(InputCount, 8, 0)
            OutputArray(InputCount, 78) = VehFuelCosts(InputCount, 8, 1)
            OutputArray(InputCount, 79) = VehFuelCosts(InputCount, 8, 3)
            OutputArray(InputCount, 80) = VehFuelCosts(InputCount, 8, 4)
            OutputArray(InputCount, 81) = VehFuelCosts(InputCount, 8, 5)
            OutputArray(InputCount, 82) = VehFuelCosts(InputCount, 8, 6)
            OutputArray(InputCount, 83) = VehFuelCosts(InputCount, 8, 7)
            OutputArray(InputCount, 84) = VehFuelCosts(InputCount, 9, 1)
            OutputArray(InputCount, 85) = VehFuelCosts(InputCount, 9, 3)
            OutputArray(InputCount, 86) = VehFuelCosts(InputCount, 9, 4)
            OutputArray(InputCount, 87) = VehFuelCosts(InputCount, 9, 5)
            OutputArray(InputCount, 88) = VehFuelCosts(InputCount, 9, 6)
            OutputArray(InputCount, 89) = VehFuelCosts(InputCount, 9, 7)
            OutputArray(InputCount, 90) = VehFuelCosts(InputCount, 9, 9)
            OutputArray(InputCount, 91) = VehFuelCosts(InputCount, 10, 1)
            OutputArray(InputCount, 92) = VehFuelCosts(InputCount, 10, 3)
            OutputArray(InputCount, 93) = VehFuelCosts(InputCount, 10, 8)
            OutputArray(InputCount, 94) = VehFuelCosts(InputCount, 10, 9)
            OutputArray(InputCount, 95) = VehFuelCosts(InputCount, 11, 1)
            OutputArray(InputCount, 96) = VehFuelCosts(InputCount, 11, 3)
            OutputArray(InputCount, 97) = VehFuelCosts(InputCount, 11, 8)
            OutputArray(InputCount, 98) = VehFuelCosts(InputCount, 11, 9)
            OutputArray(InputCount, 99) = VehFuelCosts(InputCount, 12, 0)
            OutputArray(InputCount, 100) = VehFuelCosts(InputCount, 12, 5)
            OutputArray(InputCount, 101) = VehFuelCosts(InputCount, 12, 9)
            OutputArray(InputCount, 102) = VehFuelCosts(InputCount, 13, 0)
            OutputArray(InputCount, 103) = VehFuelCosts(InputCount, 13, 1)
            OutputArray(InputCount, 104) = VehFuelCosts(InputCount, 13, 2)
            OutputArray(InputCount, 105) = VehFuelCosts(InputCount, 13, 3)
            OutputArray(InputCount, 106) = VehFuelCosts(InputCount, 13, 4)
            OutputArray(InputCount, 107) = VehFuelCosts(InputCount, 13, 5)
            OutputArray(InputCount, 108) = VehFuelCosts(InputCount, 13, 8)
            OutputArray(InputCount, 109) = VehFuelCosts(InputCount, 13, 9)
            OutputArray(InputCount, 110) = VehFuelCosts(InputCount, 14, 0)
            OutputArray(InputCount, 111) = VehFuelCosts(InputCount, 14, 1)
            OutputArray(InputCount, 112) = VehFuelCosts(InputCount, 14, 3)
            OutputArray(InputCount, 113) = VehFuelCosts(InputCount, 14, 4)
            OutputArray(InputCount, 114) = VehFuelCosts(InputCount, 14, 5)
            OutputArray(InputCount, 115) = VehFuelCosts(InputCount, 14, 6)
            OutputArray(InputCount, 116) = VehFuelCosts(InputCount, 14, 7)
            OutputArray(InputCount, 117) = VehFuelCosts(InputCount, 15, 1)
            OutputArray(InputCount, 118) = VehFuelCosts(InputCount, 15, 3)
            OutputArray(InputCount, 119) = VehFuelCosts(InputCount, 15, 4)
            OutputArray(InputCount, 120) = VehFuelCosts(InputCount, 15, 5)
            OutputArray(InputCount, 121) = VehFuelCosts(InputCount, 15, 6)
            OutputArray(InputCount, 122) = VehFuelCosts(InputCount, 15, 7)
            OutputArray(InputCount, 123) = VehFuelCosts(InputCount, 15, 9)
            OutputArray(InputCount, 124) = VehFuelCosts(InputCount, 16, 1)
            OutputArray(InputCount, 125) = VehFuelCosts(InputCount, 16, 3)
            OutputArray(InputCount, 126) = VehFuelCosts(InputCount, 16, 8)
            OutputArray(InputCount, 127) = VehFuelCosts(InputCount, 16, 9)
            OutputArray(InputCount, 128) = VehFuelCosts(InputCount, 17, 1)
            OutputArray(InputCount, 129) = VehFuelCosts(InputCount, 17, 3)
            OutputArray(InputCount, 130) = VehFuelCosts(InputCount, 17, 8)
            OutputArray(InputCount, 131) = VehFuelCosts(InputCount, 17, 9)
            OutputArray(InputCount, 132) = VehFuelCosts(InputCount, 18, 1)
            OutputArray(InputCount, 133) = VehFuelCosts(InputCount, 18, 3)
            OutputArray(InputCount, 134) = VehFuelCosts(InputCount, 18, 8)
            OutputArray(InputCount, 135) = VehFuelCosts(InputCount, 18, 9)
            OutputArray(InputCount, 136) = VehFuelCosts(InputCount, 19, 1)
            OutputArray(InputCount, 137) = VehFuelCosts(InputCount, 19, 3)
            OutputArray(InputCount, 138) = VehFuelCosts(InputCount, 19, 8)
            OutputArray(InputCount, 139) = VehFuelCosts(InputCount, 19, 9)


            InputCount += 1
        Loop

        Call WriteData("RoadLink", "ExtVar", OutputArray, , True)

    End Sub

    Sub GetCapData()

        If CapArray(CapNum, 0) Is Nothing Then
        Else

            CapID = CapArray(CapNum, 0)
            If CapArray(CapNum, 1) = "-1" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = CapArray(CapNum, 1) '- 2010 the data was in year index in the csv file, so the - 2010 was used to convert to year index. If using database, it should be in actual year
                Else
                    CapYear = CapArray(CapNum, 1)
                End If
            End If
            MLaneChange = CapArray(CapNum, 2)
            DLaneChange = CapArray(CapNum, 3)
            SLaneChange = CapArray(CapNum, 4)
            If AddingCap = False Then
                CapType = CapArray(CapNum, 5)
            End If

            CapNum += 1
        End If

    End Sub

    Sub DictionaryMissingVal()
        logarray(logNum, 0) = "No " & ErrorString & " when updating input files.  Model run terminated."
        logNum += 1
        Call WriteData("Logfile", "", logarray)
        MsgBox("Model run failed.  Please consult the log file for details.")
        End
    End Sub

    Sub CapChangeCalc()

        'start from the first row of CapArray
        CapNum = 1
        CapCount = 0
        AddingCap = False
        LanesToBuild = 0
        Do Until CapArray(CapNum, 0) Is Nothing
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
            NewCapArray(v + 1, 0) = g_modelRunID
            NewCapArray(v + 1, 1) = NewCapDetails(arraynum, 1)
            NewCapArray(v + 1, 2) = NewCapDetails(arraynum, 0)
            NewCapArray(v + 1, 3) = NewCapDetails(arraynum, 2)
            NewCapArray(v + 1, 4) = NewCapDetails(arraynum, 3)
            NewCapArray(v + 1, 5) = NewCapDetails(arraynum, 4)
            RLCapYear(v) = NewCapDetails(arraynum, 1)
        Next

    End Sub
End Module
