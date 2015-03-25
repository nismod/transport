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
    Dim RdL_InArray(,) As String
    Dim RdLEV_InArray(,) As String
    Dim RdL_OutArray(292, 139) As String
    Dim CapArray(,) As String
    Dim CapNum As Integer
    Dim NewCapArray(6835, 5) As String
    Dim yearIs2010 As Boolean = False




    Public Sub RoadLinkEVMain()

        'for year 2010, calculate as it is year 2011 and write output as year 2010
        If g_modelRunYear = 2010 Then
            'create data for year 2010
            g_modelRunYear += 1
            'Call Year2010()
            yearIs2010 = True
            'Exit Sub
        Else
            yearIs2010 = False
        End If

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
        If yearIs2010 = False And g_modelRunYear = g_initialYear Then
            'read capacity data
            Call ReadData("RoadLink", "CapChange", CapArray, g_modelRunYear)

            'do cap change calculation
            Call CapChangeCalc()

            'write all lines from NewCapArray to intermediate capacity file
            If Not NewCapArray Is Nothing Then
                Call WriteData("RoadLink", "NewCap", NewCapArray)
            End If

        End If

        'read all required new capacity for the current year
        Call ReadData("RoadLink", "NewCap", CapArray, g_modelRunYear)

        'this variable is needed to select new capacity for the interzonal model
        RoadCapNum = CapCount


        'reset NewCapArray row to the begining
        CapNum = 1
        CapID = 0
        AddingCap = True
        Call GetCapData()

        'v1.6 now calculate external variables and write output in annual timesteps
        Call CalcFlowData()

        'minus a year if it is year 2010, for the next module
        If yearIs2010 = True Then g_modelRunYear -= 1

    End Sub

    Sub GetFiles()


        'read initial input data
        Call ReadData("RoadLink", "Input", RdL_InArray, 2011)

        If g_modelRunYear <> g_initialYear Then
            'read previous year's data
            Call ReadData("RoadLink", "ExtVar", RdLEV_InArray, g_modelRunYear - 1)
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

                FlowID(InputCount, 1) = RdL_InArray(InputCount, 1)
                OZone(InputCount, 1) = RdL_InArray(InputCount, 2)
                DZone(InputCount, 1) = RdL_InArray(InputCount, 3)
                Pop1Old(InputCount, 1) = get_population_data_by_zoneID(g_modelRunYear, OZone(InputCount, 1), "OZ", "'road'")
                Pop2Old(InputCount, 1) = get_population_data_by_zoneID(g_modelRunYear, DZone(InputCount, 1), "DZ", "'road'")
                GVA1Old(InputCount, 1) = get_gva_data_by_zoneID(g_modelRunYear, OZone(InputCount, 1), "OZ", "'road'")
                GVA2Old(InputCount, 1) = get_gva_data_by_zoneID(g_modelRunYear, DZone(InputCount, 1), "DZ", "'road'")

                MLanes(InputCount, 1) = RdL_InArray(InputCount, 4)
                DLanes(InputCount, 1) = RdL_InArray(InputCount, 5)
                SLanes(InputCount, 1) = RdL_InArray(InputCount, 6)
                CostsOld(InputCount, 0) = RdL_InArray(InputCount, 28)
                MCap(InputCount, 1) = RdL_InArray(InputCount, 29)
                DCap(InputCount, 1) = RdL_InArray(InputCount, 30)
                SCap(InputCount, 1) = RdL_InArray(InputCount, 31)

                countvar = 32
                For x = 1 To 19
                    CostsOld(InputCount, x) = CDbl(RdL_InArray(InputCount, countvar))
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
                'TODO - why are you reading it from the previous year?????
                FlowID(InputCount, 1) = RdL_InArray(InputCount, 1)
                OZone(InputCount, 1) = RdL_InArray(InputCount, 2)
                DZone(InputCount, 1) = RdL_InArray(InputCount, 3)
                Pop1Old(InputCount, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, OZone(InputCount, 1), "OZ", "'road'")
                Pop2Old(InputCount, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, DZone(InputCount, 1), "DZ", "'road'")
                GVA1Old(InputCount, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, OZone(InputCount, 1), "OZ", "'road'")
                GVA2Old(InputCount, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, DZone(InputCount, 1), "DZ", "'road'")

                PetOldArr(InputCount, 1) = enearray(1, 1)
                DieOldArr(InputCount, 1) = enearray(1, 2)
                EleOldArr(InputCount, 1) = enearray(1, 3)
                LPGOldArr(InputCount, 1) = enearray(1, 4)
                CNGOldArr(InputCount, 1) = enearray(1, 5)
                HydOldArr(InputCount, 1) = enearray(1, 6)

                For x = 0 To 19
                    CostsOld(InputCount, x) = RdLEV_InArray(InputCount, x + 14)
                Next

                '030114 change
                For f = 0 To 34
                    FuelEffOld(InputCount, f) = stratarrayOLD(1, f + 33)
                Next

                'read all fuel cost for all speed categories
                VehFuelCosts(InputCount, 0, 0) = RdLEV_InArray(InputCount, 34)
                VehFuelCosts(InputCount, 0, 5) = RdLEV_InArray(InputCount, 35)
                VehFuelCosts(InputCount, 0, 9) = RdLEV_InArray(InputCount, 36)
                VehFuelCosts(InputCount, 1, 0) = RdLEV_InArray(InputCount, 37)
                VehFuelCosts(InputCount, 1, 1) = RdLEV_InArray(InputCount, 38)
                VehFuelCosts(InputCount, 1, 2) = RdLEV_InArray(InputCount, 39)
                VehFuelCosts(InputCount, 1, 3) = RdLEV_InArray(InputCount, 40)
                VehFuelCosts(InputCount, 1, 4) = RdLEV_InArray(InputCount, 41)
                VehFuelCosts(InputCount, 1, 5) = RdLEV_InArray(InputCount, 42)
                VehFuelCosts(InputCount, 1, 8) = RdLEV_InArray(InputCount, 43)
                VehFuelCosts(InputCount, 1, 9) = RdLEV_InArray(InputCount, 44)
                VehFuelCosts(InputCount, 2, 0) = RdLEV_InArray(InputCount, 45)
                VehFuelCosts(InputCount, 2, 1) = RdLEV_InArray(InputCount, 46)
                VehFuelCosts(InputCount, 2, 3) = RdLEV_InArray(InputCount, 47)
                VehFuelCosts(InputCount, 2, 4) = RdLEV_InArray(InputCount, 48)
                VehFuelCosts(InputCount, 2, 5) = RdLEV_InArray(InputCount, 49)
                VehFuelCosts(InputCount, 2, 6) = RdLEV_InArray(InputCount, 50)
                VehFuelCosts(InputCount, 2, 7) = RdLEV_InArray(InputCount, 51)
                VehFuelCosts(InputCount, 3, 1) = RdLEV_InArray(InputCount, 52)
                VehFuelCosts(InputCount, 3, 3) = RdLEV_InArray(InputCount, 53)
                VehFuelCosts(InputCount, 3, 4) = RdLEV_InArray(InputCount, 54)
                VehFuelCosts(InputCount, 3, 5) = RdLEV_InArray(InputCount, 55)
                VehFuelCosts(InputCount, 3, 6) = RdLEV_InArray(InputCount, 56)
                VehFuelCosts(InputCount, 3, 7) = RdLEV_InArray(InputCount, 57)
                VehFuelCosts(InputCount, 3, 9) = RdLEV_InArray(InputCount, 58)
                VehFuelCosts(InputCount, 4, 1) = RdLEV_InArray(InputCount, 59)
                VehFuelCosts(InputCount, 4, 3) = RdLEV_InArray(InputCount, 60)
                VehFuelCosts(InputCount, 4, 8) = RdLEV_InArray(InputCount, 61)
                VehFuelCosts(InputCount, 4, 9) = RdLEV_InArray(InputCount, 62)
                VehFuelCosts(InputCount, 5, 1) = RdLEV_InArray(InputCount, 63)
                VehFuelCosts(InputCount, 5, 3) = RdLEV_InArray(InputCount, 64)
                VehFuelCosts(InputCount, 5, 8) = RdLEV_InArray(InputCount, 65)
                VehFuelCosts(InputCount, 5, 9) = RdLEV_InArray(InputCount, 66)
                VehFuelCosts(InputCount, 6, 0) = RdLEV_InArray(InputCount, 67)
                VehFuelCosts(InputCount, 6, 5) = RdLEV_InArray(InputCount, 68)
                VehFuelCosts(InputCount, 6, 9) = RdLEV_InArray(InputCount, 69)
                VehFuelCosts(InputCount, 7, 0) = RdLEV_InArray(InputCount, 70)
                VehFuelCosts(InputCount, 7, 1) = RdLEV_InArray(InputCount, 71)
                VehFuelCosts(InputCount, 7, 2) = RdLEV_InArray(InputCount, 72)
                VehFuelCosts(InputCount, 7, 3) = RdLEV_InArray(InputCount, 73)
                VehFuelCosts(InputCount, 7, 4) = RdLEV_InArray(InputCount, 74)
                VehFuelCosts(InputCount, 7, 5) = RdLEV_InArray(InputCount, 75)
                VehFuelCosts(InputCount, 7, 8) = RdLEV_InArray(InputCount, 76)
                VehFuelCosts(InputCount, 7, 9) = RdLEV_InArray(InputCount, 77)
                VehFuelCosts(InputCount, 8, 0) = RdLEV_InArray(InputCount, 78)
                VehFuelCosts(InputCount, 8, 1) = RdLEV_InArray(InputCount, 79)
                VehFuelCosts(InputCount, 8, 3) = RdLEV_InArray(InputCount, 80)
                VehFuelCosts(InputCount, 8, 4) = RdLEV_InArray(InputCount, 81)
                VehFuelCosts(InputCount, 8, 5) = RdLEV_InArray(InputCount, 82)
                VehFuelCosts(InputCount, 8, 6) = RdLEV_InArray(InputCount, 83)
                VehFuelCosts(InputCount, 8, 7) = RdLEV_InArray(InputCount, 84)
                VehFuelCosts(InputCount, 9, 1) = RdLEV_InArray(InputCount, 85)
                VehFuelCosts(InputCount, 9, 3) = RdLEV_InArray(InputCount, 86)
                VehFuelCosts(InputCount, 9, 4) = RdLEV_InArray(InputCount, 87)
                VehFuelCosts(InputCount, 9, 5) = RdLEV_InArray(InputCount, 88)
                VehFuelCosts(InputCount, 9, 6) = RdLEV_InArray(InputCount, 89)
                VehFuelCosts(InputCount, 9, 7) = RdLEV_InArray(InputCount, 90)
                VehFuelCosts(InputCount, 9, 9) = RdLEV_InArray(InputCount, 91)
                VehFuelCosts(InputCount, 10, 1) = RdLEV_InArray(InputCount, 92)
                VehFuelCosts(InputCount, 10, 3) = RdLEV_InArray(InputCount, 93)
                VehFuelCosts(InputCount, 10, 8) = RdLEV_InArray(InputCount, 94)
                VehFuelCosts(InputCount, 10, 9) = RdLEV_InArray(InputCount, 95)
                VehFuelCosts(InputCount, 11, 1) = RdLEV_InArray(InputCount, 96)
                VehFuelCosts(InputCount, 11, 3) = RdLEV_InArray(InputCount, 97)
                VehFuelCosts(InputCount, 11, 8) = RdLEV_InArray(InputCount, 98)
                VehFuelCosts(InputCount, 11, 9) = RdLEV_InArray(InputCount, 99)
                VehFuelCosts(InputCount, 12, 0) = RdLEV_InArray(InputCount, 100)
                VehFuelCosts(InputCount, 12, 5) = RdLEV_InArray(InputCount, 101)
                VehFuelCosts(InputCount, 12, 9) = RdLEV_InArray(InputCount, 102)
                VehFuelCosts(InputCount, 13, 0) = RdLEV_InArray(InputCount, 103)
                VehFuelCosts(InputCount, 13, 1) = RdLEV_InArray(InputCount, 104)
                VehFuelCosts(InputCount, 13, 2) = RdLEV_InArray(InputCount, 105)
                VehFuelCosts(InputCount, 13, 3) = RdLEV_InArray(InputCount, 106)
                VehFuelCosts(InputCount, 13, 4) = RdLEV_InArray(InputCount, 107)
                VehFuelCosts(InputCount, 13, 5) = RdLEV_InArray(InputCount, 108)
                VehFuelCosts(InputCount, 13, 8) = RdLEV_InArray(InputCount, 109)
                VehFuelCosts(InputCount, 13, 9) = RdLEV_InArray(InputCount, 110)
                VehFuelCosts(InputCount, 14, 0) = RdLEV_InArray(InputCount, 111)
                VehFuelCosts(InputCount, 14, 1) = RdLEV_InArray(InputCount, 112)
                VehFuelCosts(InputCount, 14, 3) = RdLEV_InArray(InputCount, 113)
                VehFuelCosts(InputCount, 14, 4) = RdLEV_InArray(InputCount, 114)
                VehFuelCosts(InputCount, 14, 5) = RdLEV_InArray(InputCount, 115)
                VehFuelCosts(InputCount, 14, 6) = RdLEV_InArray(InputCount, 116)
                VehFuelCosts(InputCount, 14, 7) = RdLEV_InArray(InputCount, 117)
                VehFuelCosts(InputCount, 15, 1) = RdLEV_InArray(InputCount, 118)
                VehFuelCosts(InputCount, 15, 3) = RdLEV_InArray(InputCount, 119)
                VehFuelCosts(InputCount, 15, 4) = RdLEV_InArray(InputCount, 120)
                VehFuelCosts(InputCount, 15, 5) = RdLEV_InArray(InputCount, 121)
                VehFuelCosts(InputCount, 15, 6) = RdLEV_InArray(InputCount, 122)
                VehFuelCosts(InputCount, 15, 7) = RdLEV_InArray(InputCount, 123)
                VehFuelCosts(InputCount, 15, 9) = RdLEV_InArray(InputCount, 124)
                VehFuelCosts(InputCount, 16, 1) = RdLEV_InArray(InputCount, 125)
                VehFuelCosts(InputCount, 16, 3) = RdLEV_InArray(InputCount, 126)
                VehFuelCosts(InputCount, 16, 8) = RdLEV_InArray(InputCount, 127)
                VehFuelCosts(InputCount, 16, 9) = RdLEV_InArray(InputCount, 128)
                VehFuelCosts(InputCount, 17, 1) = RdLEV_InArray(InputCount, 129)
                VehFuelCosts(InputCount, 17, 3) = RdLEV_InArray(InputCount, 130)
                VehFuelCosts(InputCount, 17, 8) = RdLEV_InArray(InputCount, 131)
                VehFuelCosts(InputCount, 17, 9) = RdLEV_InArray(InputCount, 132)
                VehFuelCosts(InputCount, 18, 1) = RdLEV_InArray(InputCount, 133)
                VehFuelCosts(InputCount, 18, 3) = RdLEV_InArray(InputCount, 134)
                VehFuelCosts(InputCount, 18, 8) = RdLEV_InArray(InputCount, 135)
                VehFuelCosts(InputCount, 18, 9) = RdLEV_InArray(InputCount, 136)
                VehFuelCosts(InputCount, 19, 1) = RdLEV_InArray(InputCount, 137)
                VehFuelCosts(InputCount, 19, 3) = RdLEV_InArray(InputCount, 138)
                VehFuelCosts(InputCount, 19, 8) = RdLEV_InArray(InputCount, 139)
                VehFuelCosts(InputCount, 19, 9) = RdLEV_InArray(InputCount, 140)

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
                Pop1New = get_population_data_by_zoneID(g_modelRunYear, OZone(InputCount, 1), "OZ", "'road'")
                Pop2New = get_population_data_by_zoneID(g_modelRunYear, DZone(InputCount, 1), "DZ", "'road'")
            End If
            'TODO - Why is this not pulling from the GVA functions??? -this is for the runs without database, can be deleted if in the database version
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
                GVA1New = get_gva_data_by_zoneID(g_modelRunYear, OZone(InputCount, 1), "OZ", "'road'")
                GVA2New = get_gva_data_by_zoneID(g_modelRunYear, DZone(InputCount, 1), "DZ", "'road'")
            End If
            If RdLEneSource = "Constant" Then
                For x = 0 To 19
                    CostsNew(x) = CostsOld(InputCount, x) * CostGrowth
                Next
            ElseIf RdLEneSource = "File" Then
                'not set up for scaling files
            ElseIf RdLEneSource = "Database" Then
                PetNew = enearray(2, 1)
                DieNew = enearray(2, 2)
                EleNew = enearray(2, 3)
                LPGNew = enearray(2, 4)
                CNGNew = enearray(2, 5)
                HydNew = enearray(2, 6)
                'calculate ratio for each fuel
                PetRat = PetNew / PetOldArr(InputCount, 1)
                DieRat = DieNew / DieOldArr(InputCount, 1)
                EleRat = EleNew / EleOldArr(InputCount, 1)
                LPGRat = LPGNew / LPGOldArr(InputCount, 1)
                CNGRat = CNGNew / CNGOldArr(InputCount, 1)
                HydRat = HydNew / HydOldArr(InputCount, 1)
                '0301014 corrected fuel efficiency change calculation - was previously just multiplying by figure straight from strategy array (which meant that fuel costs quickly declined to zero)
                For f = 0 To 34
                    FuelEffNew(InputCount, f) = stratarray(1, f + 33)
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
                        CarbCharge(0, 0) = (0.04 * stratarray(1, 65) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(0, 5) = (0.032 * stratarray(1, 66) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(0, 9) = (0.123 * stratarray(1, 67) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(1, 0) = (0.086 * stratarray(1, 33) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(1, 1) = (0.057 * stratarray(1, 34) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(1, 2) = (0.056 * stratarray(1, 45) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(1, 3) = (0.038 * stratarray(1, 46) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(1, 4) = (0.06 * stratarray(1, 47) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(1, 5) = (0.165 * stratarray(1, 35) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(1, 8) = (0.438 * stratarray(1, 48) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(1, 9) = (0.178 * stratarray(1, 49) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(2, 0) = (0.088 * stratarray(1, 36) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(2, 1) = (0.079 * stratarray(1, 37) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(2, 3) = (0.044 * stratarray(1, 50) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(2, 4) = (0.058 * stratarray(1, 51) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(2, 5) = (0.562 * stratarray(1, 38) * stratarray(1, 76) * (stratarray(1, 73) / 10))
                        CarbCharge(2, 6) = (0.118 * stratarray(1, 52) * stratarray(1, 77) * (stratarray(1, 73) / 10))
                        CarbCharge(2, 7) = (0.808 * stratarray(1, 53) * stratarray(1, 78) * (stratarray(1, 73) / 10))
                        CarbCharge(3, 1) = (0.176 * stratarray(1, 43) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(3, 3) = (0.185 * stratarray(1, 54) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(3, 4) = (0.119 * stratarray(1, 55) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(3, 5) = (0.2554 * stratarray(1, 44) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(3, 6) = (0.954 * stratarray(1, 56) * stratarray(1, 77) * (stratarray(1, 73) / 10))
                        CarbCharge(3, 7) = (3.749 * stratarray(1, 57) * stratarray(1, 78) * (stratarray(1, 73) / 10))
                        CarbCharge(3, 9) = (0.546 * stratarray(1, 58) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(4, 1) = (0.259 * stratarray(1, 39) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(4, 3) = (0.15 * stratarray(1, 59) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(4, 8) = (0.957 * stratarray(1, 60) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(4, 9) = (0.898 * stratarray(1, 61) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(5, 1) = (0.376 * stratarray(1, 41) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(5, 3) = (0.221 * stratarray(1, 62) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(5, 8) = (1.398 * stratarray(1, 63) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(5, 9) = (1.123 * stratarray(1, 64) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(6, 0) = (0.04 * stratarray(1, 65) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(6, 5) = (0.032 * stratarray(1, 66) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(6, 9) = (0.123 * stratarray(1, 67) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(7, 0) = (0.086 * stratarray(1, 33) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(7, 1) = (0.057 * stratarray(1, 35) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(7, 2) = (0.056 * stratarray(1, 45) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(7, 3) = (0.038 * stratarray(1, 46) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(7, 4) = (0.06 * stratarray(1, 47) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(7, 5) = (0.165 * stratarray(1, 35) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(7, 8) = (0.438 * stratarray(1, 48) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(7, 9) = (0.178 * stratarray(1, 49) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(8, 0) = (0.088 * stratarray(1, 36) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(8, 1) = (0.079 * stratarray(1, 37) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(8, 3) = (0.044 * stratarray(1, 50) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(8, 4) = (0.058 * stratarray(1, 51) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(8, 5) = (0.562 * stratarray(1, 38) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(8, 6) = (0.118 * stratarray(1, 52) * stratarray(1, 77) * (stratarray(1, 73) / 10))
                        CarbCharge(8, 7) = (0.808 * stratarray(1, 53) * stratarray(1, 78) * (stratarray(1, 73) / 10))
                        CarbCharge(9, 1) = (0.176 * stratarray(1, 43) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(9, 3) = (0.185 * stratarray(1, 54) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(9, 4) = (0.119 * stratarray(1, 55) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(9, 5) = (2.554 * stratarray(1, 44) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(9, 6) = (0.954 * stratarray(1, 56) * stratarray(1, 77) * (stratarray(1, 73) / 10))
                        CarbCharge(9, 7) = (3.749 * stratarray(1, 57) * stratarray(1, 78) * (stratarray(1, 73) / 10))
                        CarbCharge(9, 9) = (0.546 * stratarray(1, 58) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(10, 1) = (0.259 * stratarray(1, 39) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(10, 3) = (0.15 * stratarray(1, 59) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(10, 8) = (0.957 * stratarray(1, 60) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(10, 9) = (0.898 * stratarray(1, 61) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(11, 1) = (0.376 * stratarray(1, 41) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(11, 3) = (0.221 * stratarray(1, 62) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(11, 8) = (1.398 * stratarray(1, 63) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(11, 9) = (1.123 * stratarray(1, 64) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(12, 0) = (0.04 * stratarray(1, 65) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(12, 5) = (0.032 * stratarray(1, 66) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(12, 9) = (0.123 * stratarray(1, 67) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(13, 0) = (0.086 * stratarray(1, 33) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(13, 1) = (0.057 * stratarray(1, 34) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(13, 2) = (0.056 * stratarray(1, 45) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(13, 3) = (0.038 * stratarray(1, 46) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(13, 4) = (0.016 * stratarray(1, 47) * stratarray(1, 74) * (stratarray(1, 72) / 10))
                        CarbCharge(13, 5) = (0.165 * stratarray(1, 35) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(13, 8) = (0.438 * stratarray(1, 48) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(13, 9) = (0.178 * stratarray(1, 49) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(14, 0) = (0.088 * stratarray(1, 36) * stratarray(1, 74) * (stratarray(1, 73) / 10))
                        CarbCharge(14, 1) = (0.079 * stratarray(1, 37) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(14, 3) = (0.044 * stratarray(1, 50) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(14, 4) = (0.423 * stratarray(1, 51) * stratarray(1, 75) * (stratarray(1, 72) / 10))
                        CarbCharge(14, 5) = (0.562 * stratarray(1, 38) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(14, 6) = (0.118 * stratarray(1, 52) * stratarray(1, 77) * (stratarray(1, 73) / 10))
                        CarbCharge(14, 7) = (0.808 * stratarray(1, 53) * stratarray(1, 78) * (stratarray(1, 73) / 10))
                        CarbCharge(15, 1) = (0.196 * stratarray(1, 43) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(15, 3) = (0.119 * stratarray(1, 54) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(15, 4) = (1.037 * stratarray(1, 55) * stratarray(1, 75) * (stratarray(1, 72) / 10))
                        CarbCharge(15, 5) = (1.7 * stratarray(1, 44) * stratarray(1, 76) * (stratarray(1, 72) / 10))
                        CarbCharge(15, 6) = (0.364 * stratarray(1, 56) * stratarray(1, 77) * (stratarray(1, 73) / 10))
                        CarbCharge(15, 7) = (6.283 * stratarray(1, 57) * stratarray(1, 78) * (stratarray(1, 73) / 10))
                        CarbCharge(15, 9) = (0.546 * stratarray(1, 58) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(16, 1) = (0.259 * stratarray(1, 39) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(16, 3) = (0.15 * stratarray(1, 59) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(16, 8) = (0.957 * stratarray(1, 60) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(16, 9) = (0.898 * stratarray(1, 61) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(17, 1) = (0.259 * stratarray(1, 39) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(17, 3) = (0.15 * stratarray(1, 59) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(17, 8) = (0.957 * stratarray(1, 60) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(17, 9) = (0.898 * stratarray(1, 61) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(18, 1) = (0.376 * stratarray(1, 41) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(18, 3) = (0.221 * stratarray(1, 62) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(18, 8) = (1.398 * stratarray(1, 63) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(18, 9) = (1.123 * stratarray(1, 64) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(19, 1) = (0.376 * stratarray(1, 41) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(19, 3) = (0.221 * stratarray(1, 62) * stratarray(1, 75) * (stratarray(1, 73) / 10))
                        CarbCharge(19, 8) = (1.398 * stratarray(1, 63) * stratarray(1, 79) * (stratarray(1, 73) / 10))
                        CarbCharge(19, 9) = (1.123 * stratarray(1, 64) * stratarray(1, 79) * (stratarray(1, 73) / 10))
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
                CostsNew(0) = (VehCosts(0, 0) * stratarray(1, 13)) + (VehCosts(0, 5) * stratarray(1, 14)) + (VehCosts(0, 9) * stratarray(1, 15))
                CostsNew(1) = (VehCosts(1, 0) * stratarray(1, 3)) + (VehCosts(1, 1) * stratarray(1, 4)) + (VehCosts(1, 2) * stratarray(1, 16)) + (VehCosts(1, 3) * stratarray(1, 17)) + (VehCosts(1, 4) * stratarray(1, 18)) + (VehCosts(1, 5) * stratarray(1, 5)) + (VehCosts(1, 8) * stratarray(1, 19)) + (VehCosts(1, 9) * stratarray(1, 20))
                CostsNew(2) = (VehCosts(2, 0) * stratarray(1, 6)) + (VehCosts(2, 1) * stratarray(1, 7)) + (VehCosts(2, 3) * stratarray(1, 21)) + (VehCosts(2, 4) * stratarray(1, 22)) + (VehCosts(2, 5) * stratarray(1, 8)) + (VehCosts(2, 6) * stratarray(1, 23)) + (VehCosts(2, 7) * stratarray(1, 24))
                CostsNew(3) = (VehCosts(3, 1) * stratarray(1, 11)) + (VehCosts(3, 3) * stratarray(1, 25)) + (VehCosts(3, 4) * stratarray(1, 24)) + (VehCosts(3, 5) * stratarray(1, 12)) + (VehCosts(3, 6) * stratarray(1, 27)) + (VehCosts(3, 7) * stratarray(1, 28)) + (VehCosts(3, 9) * stratarray(1, 29))
                CostsNew(4) = (VehCosts(4, 1) * stratarray(1, 9)) + (VehCosts(4, 3) * stratarray(1, 30)) + (VehCosts(4, 8) * stratarray(1, 31)) + (VehCosts(4, 9) * stratarray(1, 32))
                CostsNew(5) = (VehCosts(5, 1) * stratarray(1, 9)) + (VehCosts(5, 3) * stratarray(1, 30)) + (VehCosts(5, 8) * stratarray(1, 31)) + (VehCosts(5, 9) * stratarray(1, 32))
                CostsNew(6) = (VehCosts(6, 0) * stratarray(1, 13)) + (VehCosts(6, 5) * stratarray(1, 14)) + (VehCosts(6, 9) * stratarray(1, 15))
                CostsNew(7) = (VehCosts(7, 0) * stratarray(1, 3)) + (VehCosts(7, 1) * stratarray(1, 4)) + (VehCosts(7, 2) * stratarray(1, 16)) + (VehCosts(7, 3) * stratarray(1, 17)) + (VehCosts(7, 4) * stratarray(1, 18)) + (VehCosts(7, 5) * stratarray(1, 5)) + (VehCosts(7, 8) * stratarray(1, 19)) + (VehCosts(7, 9) * stratarray(1, 20))
                CostsNew(8) = (VehCosts(8, 0) * stratarray(1, 6)) + (VehCosts(8, 1) * stratarray(1, 7)) + (VehCosts(8, 3) * stratarray(1, 21)) + (VehCosts(8, 4) * stratarray(1, 22)) + (VehCosts(8, 5) * stratarray(1, 8)) + (VehCosts(8, 6) * stratarray(1, 23)) + (VehCosts(8, 7) * stratarray(1, 24))
                CostsNew(9) = (VehCosts(9, 1) * stratarray(1, 11)) + (VehCosts(9, 3) * stratarray(1, 25)) + (VehCosts(9, 4) * stratarray(1, 26)) + (VehCosts(9, 5) * stratarray(1, 12)) + (VehCosts(9, 6) * stratarray(1, 27)) + (VehCosts(9, 7) * stratarray(1, 28)) + (VehCosts(9, 9) * stratarray(1, 29))
                CostsNew(10) = (VehCosts(10, 1) * stratarray(1, 9)) + (VehCosts(10, 3) * stratarray(1, 30)) + (VehCosts(10, 8) * stratarray(1, 31)) + (VehCosts(10, 9) * stratarray(1, 32))
                CostsNew(11) = (VehCosts(11, 1) * stratarray(1, 9)) + (VehCosts(11, 3) * stratarray(1, 30)) + (VehCosts(11, 8) * stratarray(1, 31)) + (VehCosts(11, 9) * stratarray(1, 32))
                CostsNew(12) = (VehCosts(12, 0) * stratarray(1, 13)) + (VehCosts(12, 5) * stratarray(1, 14)) + (VehCosts(12, 9) * stratarray(1, 15))
                CostsNew(13) = (VehCosts(13, 0) * stratarray(1, 3)) + (VehCosts(13, 1) * stratarray(1, 4)) + (VehCosts(13, 2) * stratarray(1, 16)) + (VehCosts(13, 3) * stratarray(1, 17)) + (VehCosts(13, 4) * stratarray(1, 18)) + (VehCosts(13, 5) * stratarray(1, 5)) + (VehCosts(13, 8) * stratarray(1, 19)) + (VehCosts(13, 9) * stratarray(1, 20))
                CostsNew(14) = (VehCosts(14, 0) * stratarray(1, 6)) + (VehCosts(14, 1) * stratarray(1, 7)) + (VehCosts(14, 3) * stratarray(1, 21)) + (VehCosts(14, 4) * stratarray(1, 22)) + (VehCosts(14, 5) * stratarray(1, 8)) + (VehCosts(14, 6) * stratarray(1, 23)) + (VehCosts(14, 7) * stratarray(1, 24))
                CostsNew(15) = (VehCosts(15, 1) * stratarray(1, 11)) + (VehCosts(15, 3) * stratarray(1, 25)) + (VehCosts(15, 4) * stratarray(1, 26)) + (VehCosts(15, 5) * stratarray(1, 12)) + (VehCosts(15, 6) * stratarray(1, 27)) + (VehCosts(15, 7) * stratarray(1, 28)) + (VehCosts(15, 9) * stratarray(1, 29))
                CostsNew(16) = (VehCosts(16, 1) * stratarray(1, 9)) + (VehCosts(16, 3) * stratarray(1, 30)) + (VehCosts(16, 8) * stratarray(1, 31)) + (VehCosts(16, 9) * stratarray(1, 32))
                CostsNew(17) = (VehCosts(17, 1) * stratarray(1, 9)) + (VehCosts(17, 3) * stratarray(1, 30)) + (VehCosts(17, 8) * stratarray(1, 31)) + (VehCosts(17, 9) * stratarray(1, 32))
                CostsNew(18) = (VehCosts(18, 1) * stratarray(1, 9)) + (VehCosts(18, 3) * stratarray(1, 30)) + (VehCosts(18, 8) * stratarray(1, 31)) + (VehCosts(18, 9) * stratarray(1, 32))
                CostsNew(19) = (VehCosts(19, 1) * stratarray(1, 9)) + (VehCosts(19, 3) * stratarray(1, 30)) + (VehCosts(19, 8) * stratarray(1, 31)) + (VehCosts(19, 9) * stratarray(1, 32))
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
            MCap(InputCount, 1) = stratarray(1, 81)
            DCap(InputCount, 1) = stratarray(1, 82)
            SCap(InputCount, 1) = stratarray(1, 83)

            'minus a year and write data as 2010 if year is 2010
            If yearIs2010 = True Then g_modelRunYear -= 1

            'write to output file
            RdL_OutArray(InputCount, 0) = g_modelRunID
            RdL_OutArray(InputCount, 1) = FlowID(InputCount, 1)
            RdL_OutArray(InputCount, 2) = g_modelRunYear
            RdL_OutArray(InputCount, 3) = Pop1New
            RdL_OutArray(InputCount, 4) = Pop2New
            RdL_OutArray(InputCount, 5) = GVA1New
            RdL_OutArray(InputCount, 6) = GVA2New
            RdL_OutArray(InputCount, 7) = MLanes(InputCount, 1)
            RdL_OutArray(InputCount, 8) = DLanes(InputCount, 1)
            RdL_OutArray(InputCount, 9) = SLanes(InputCount, 1)
            RdL_OutArray(InputCount, 10) = MCap(InputCount, 1)
            RdL_OutArray(InputCount, 11) = DCap(InputCount, 1)
            RdL_OutArray(InputCount, 12) = SCap(InputCount, 1)
            For x = 0 To 19
                RdL_OutArray(InputCount, 13 + x) = CostsNew(x)
            Next

            'write all speed category costs to database
            RdL_OutArray(InputCount, 33) = VehFuelCosts(InputCount, 0, 0)
            RdL_OutArray(InputCount, 34) = VehFuelCosts(InputCount, 0, 5)
            RdL_OutArray(InputCount, 35) = VehFuelCosts(InputCount, 0, 9)
            RdL_OutArray(InputCount, 36) = VehFuelCosts(InputCount, 1, 0)
            RdL_OutArray(InputCount, 37) = VehFuelCosts(InputCount, 1, 1)
            RdL_OutArray(InputCount, 38) = VehFuelCosts(InputCount, 1, 2)
            RdL_OutArray(InputCount, 39) = VehFuelCosts(InputCount, 1, 3)
            RdL_OutArray(InputCount, 40) = VehFuelCosts(InputCount, 1, 4)
            RdL_OutArray(InputCount, 41) = VehFuelCosts(InputCount, 1, 5)
            RdL_OutArray(InputCount, 42) = VehFuelCosts(InputCount, 1, 8)
            RdL_OutArray(InputCount, 43) = VehFuelCosts(InputCount, 1, 9)
            RdL_OutArray(InputCount, 44) = VehFuelCosts(InputCount, 2, 0)
            RdL_OutArray(InputCount, 45) = VehFuelCosts(InputCount, 2, 1)
            RdL_OutArray(InputCount, 46) = VehFuelCosts(InputCount, 2, 3)
            RdL_OutArray(InputCount, 47) = VehFuelCosts(InputCount, 2, 4)
            RdL_OutArray(InputCount, 48) = VehFuelCosts(InputCount, 2, 5)
            RdL_OutArray(InputCount, 49) = VehFuelCosts(InputCount, 2, 6)
            RdL_OutArray(InputCount, 50) = VehFuelCosts(InputCount, 2, 7)
            RdL_OutArray(InputCount, 51) = VehFuelCosts(InputCount, 3, 1)
            RdL_OutArray(InputCount, 52) = VehFuelCosts(InputCount, 3, 3)
            RdL_OutArray(InputCount, 53) = VehFuelCosts(InputCount, 3, 4)
            RdL_OutArray(InputCount, 54) = VehFuelCosts(InputCount, 3, 5)
            RdL_OutArray(InputCount, 55) = VehFuelCosts(InputCount, 3, 6)
            RdL_OutArray(InputCount, 56) = VehFuelCosts(InputCount, 3, 7)
            RdL_OutArray(InputCount, 57) = VehFuelCosts(InputCount, 3, 9)
            RdL_OutArray(InputCount, 58) = VehFuelCosts(InputCount, 4, 1)
            RdL_OutArray(InputCount, 59) = VehFuelCosts(InputCount, 4, 3)
            RdL_OutArray(InputCount, 60) = VehFuelCosts(InputCount, 4, 8)
            RdL_OutArray(InputCount, 61) = VehFuelCosts(InputCount, 4, 9)
            RdL_OutArray(InputCount, 62) = VehFuelCosts(InputCount, 5, 1)
            RdL_OutArray(InputCount, 63) = VehFuelCosts(InputCount, 5, 3)
            RdL_OutArray(InputCount, 64) = VehFuelCosts(InputCount, 5, 8)
            RdL_OutArray(InputCount, 65) = VehFuelCosts(InputCount, 5, 9)
            RdL_OutArray(InputCount, 66) = VehFuelCosts(InputCount, 6, 0)
            RdL_OutArray(InputCount, 67) = VehFuelCosts(InputCount, 6, 5)
            RdL_OutArray(InputCount, 68) = VehFuelCosts(InputCount, 6, 9)
            RdL_OutArray(InputCount, 69) = VehFuelCosts(InputCount, 7, 0)
            RdL_OutArray(InputCount, 70) = VehFuelCosts(InputCount, 7, 1)
            RdL_OutArray(InputCount, 71) = VehFuelCosts(InputCount, 7, 2)
            RdL_OutArray(InputCount, 72) = VehFuelCosts(InputCount, 7, 3)
            RdL_OutArray(InputCount, 73) = VehFuelCosts(InputCount, 7, 4)
            RdL_OutArray(InputCount, 74) = VehFuelCosts(InputCount, 7, 5)
            RdL_OutArray(InputCount, 75) = VehFuelCosts(InputCount, 7, 8)
            RdL_OutArray(InputCount, 76) = VehFuelCosts(InputCount, 7, 9)
            RdL_OutArray(InputCount, 77) = VehFuelCosts(InputCount, 8, 0)
            RdL_OutArray(InputCount, 78) = VehFuelCosts(InputCount, 8, 1)
            RdL_OutArray(InputCount, 79) = VehFuelCosts(InputCount, 8, 3)
            RdL_OutArray(InputCount, 80) = VehFuelCosts(InputCount, 8, 4)
            RdL_OutArray(InputCount, 81) = VehFuelCosts(InputCount, 8, 5)
            RdL_OutArray(InputCount, 82) = VehFuelCosts(InputCount, 8, 6)
            RdL_OutArray(InputCount, 83) = VehFuelCosts(InputCount, 8, 7)
            RdL_OutArray(InputCount, 84) = VehFuelCosts(InputCount, 9, 1)
            RdL_OutArray(InputCount, 85) = VehFuelCosts(InputCount, 9, 3)
            RdL_OutArray(InputCount, 86) = VehFuelCosts(InputCount, 9, 4)
            RdL_OutArray(InputCount, 87) = VehFuelCosts(InputCount, 9, 5)
            RdL_OutArray(InputCount, 88) = VehFuelCosts(InputCount, 9, 6)
            RdL_OutArray(InputCount, 89) = VehFuelCosts(InputCount, 9, 7)
            RdL_OutArray(InputCount, 90) = VehFuelCosts(InputCount, 9, 9)
            RdL_OutArray(InputCount, 91) = VehFuelCosts(InputCount, 10, 1)
            RdL_OutArray(InputCount, 92) = VehFuelCosts(InputCount, 10, 3)
            RdL_OutArray(InputCount, 93) = VehFuelCosts(InputCount, 10, 8)
            RdL_OutArray(InputCount, 94) = VehFuelCosts(InputCount, 10, 9)
            RdL_OutArray(InputCount, 95) = VehFuelCosts(InputCount, 11, 1)
            RdL_OutArray(InputCount, 96) = VehFuelCosts(InputCount, 11, 3)
            RdL_OutArray(InputCount, 97) = VehFuelCosts(InputCount, 11, 8)
            RdL_OutArray(InputCount, 98) = VehFuelCosts(InputCount, 11, 9)
            RdL_OutArray(InputCount, 99) = VehFuelCosts(InputCount, 12, 0)
            RdL_OutArray(InputCount, 100) = VehFuelCosts(InputCount, 12, 5)
            RdL_OutArray(InputCount, 101) = VehFuelCosts(InputCount, 12, 9)
            RdL_OutArray(InputCount, 102) = VehFuelCosts(InputCount, 13, 0)
            RdL_OutArray(InputCount, 103) = VehFuelCosts(InputCount, 13, 1)
            RdL_OutArray(InputCount, 104) = VehFuelCosts(InputCount, 13, 2)
            RdL_OutArray(InputCount, 105) = VehFuelCosts(InputCount, 13, 3)
            RdL_OutArray(InputCount, 106) = VehFuelCosts(InputCount, 13, 4)
            RdL_OutArray(InputCount, 107) = VehFuelCosts(InputCount, 13, 5)
            RdL_OutArray(InputCount, 108) = VehFuelCosts(InputCount, 13, 8)
            RdL_OutArray(InputCount, 109) = VehFuelCosts(InputCount, 13, 9)
            RdL_OutArray(InputCount, 110) = VehFuelCosts(InputCount, 14, 0)
            RdL_OutArray(InputCount, 111) = VehFuelCosts(InputCount, 14, 1)
            RdL_OutArray(InputCount, 112) = VehFuelCosts(InputCount, 14, 3)
            RdL_OutArray(InputCount, 113) = VehFuelCosts(InputCount, 14, 4)
            RdL_OutArray(InputCount, 114) = VehFuelCosts(InputCount, 14, 5)
            RdL_OutArray(InputCount, 115) = VehFuelCosts(InputCount, 14, 6)
            RdL_OutArray(InputCount, 116) = VehFuelCosts(InputCount, 14, 7)
            RdL_OutArray(InputCount, 117) = VehFuelCosts(InputCount, 15, 1)
            RdL_OutArray(InputCount, 118) = VehFuelCosts(InputCount, 15, 3)
            RdL_OutArray(InputCount, 119) = VehFuelCosts(InputCount, 15, 4)
            RdL_OutArray(InputCount, 120) = VehFuelCosts(InputCount, 15, 5)
            RdL_OutArray(InputCount, 121) = VehFuelCosts(InputCount, 15, 6)
            RdL_OutArray(InputCount, 122) = VehFuelCosts(InputCount, 15, 7)
            RdL_OutArray(InputCount, 123) = VehFuelCosts(InputCount, 15, 9)
            RdL_OutArray(InputCount, 124) = VehFuelCosts(InputCount, 16, 1)
            RdL_OutArray(InputCount, 125) = VehFuelCosts(InputCount, 16, 3)
            RdL_OutArray(InputCount, 126) = VehFuelCosts(InputCount, 16, 8)
            RdL_OutArray(InputCount, 127) = VehFuelCosts(InputCount, 16, 9)
            RdL_OutArray(InputCount, 128) = VehFuelCosts(InputCount, 17, 1)
            RdL_OutArray(InputCount, 129) = VehFuelCosts(InputCount, 17, 3)
            RdL_OutArray(InputCount, 130) = VehFuelCosts(InputCount, 17, 8)
            RdL_OutArray(InputCount, 131) = VehFuelCosts(InputCount, 17, 9)
            RdL_OutArray(InputCount, 132) = VehFuelCosts(InputCount, 18, 1)
            RdL_OutArray(InputCount, 133) = VehFuelCosts(InputCount, 18, 3)
            RdL_OutArray(InputCount, 134) = VehFuelCosts(InputCount, 18, 8)
            RdL_OutArray(InputCount, 135) = VehFuelCosts(InputCount, 18, 9)
            RdL_OutArray(InputCount, 136) = VehFuelCosts(InputCount, 19, 1)
            RdL_OutArray(InputCount, 137) = VehFuelCosts(InputCount, 19, 3)
            RdL_OutArray(InputCount, 138) = VehFuelCosts(InputCount, 19, 8)
            RdL_OutArray(InputCount, 139) = VehFuelCosts(InputCount, 19, 9)

            'add back a year for next zone/link
            If yearIs2010 = True Then g_modelRunYear += 1

            InputCount += 1
        Loop

        Call WriteData("RoadLink", "ExtVar", RdL_OutArray, , True)

    End Sub

    Sub GetCapData()

        If Not CapArray Is Nothing Then

            CapID = CapArray(CapNum, 3)
            If CapArray(CapNum, 2) = g_initialYear Then
                CapYear = g_initialYear
            Else
                If AddingCap = False Then
                    CapYear = CapArray(CapNum, 2) '- 2010 the data was in year index in the csv file, so the - 2010 was used to convert to year index. If using database, it should be in actual year
                Else
                    CapYear = CapArray(CapNum, 2)
                End If
            End If
            MLaneChange = CapArray(CapNum, 4)
            DLaneChange = CapArray(CapNum, 5)
            SLaneChange = CapArray(CapNum, 6)
            If AddingCap = False Then
                CapType = CapArray(CapNum, 7)
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
        'addingcap is false when is reading from LU table
        AddingCap = False
        LanesToBuild = 0

        If CapArray Is Nothing Then Exit Sub

        Do Until CapArray(CapNum, 0) Is Nothing

            If Not CapArray Is Nothing Then

                CapID = CapArray(CapNum, 1)
                If CapArray(CapNum, 2) = g_initialYear Then
                    CapYear = g_initialYear
                Else
                    If AddingCap = False Then
                        CapYear = CapArray(CapNum, 2) '- 2010 the data was in year index in the csv file, so the - 2010 was used to convert to year index. If using database, it should be in actual year
                    Else
                        CapYear = CapArray(CapNum, 2)
                    End If
                End If
                MLaneChange = CapArray(CapNum, 3)
                DLaneChange = CapArray(CapNum, 4)
                SLaneChange = CapArray(CapNum, 5)
                If AddingCap = False Then
                    CapType = CapArray(CapNum, 6)
                End If

                CapNum += 1
            End If

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
                        If CapYear > g_initialYear Then
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
            'NewCapArray(v + 1, 6) = CapType
        Next

    End Sub
End Module
