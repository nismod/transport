Module IntraZoneRoadModelB
    '1.3 this version adds in fuel consumption estimation
    'This version adds in a capacity constraint, and is dependent on module FullCDAM for file paths
    'It now also allows elasticities to vary over time, and reads them from an input file
    'also now includes the option to use variable elasticities
    'also now includes option to capture effect of smarter choices, smart logistics and smart urban logistics
    '1.4 revises the speed-flow relationship, and in order to do this splits the forecasts into four road types
    'now also has the option to build new capacity
    'also now can take account of new capacity constructed as result of strategy (ie not just TR1)
    'now also includes variable trip rate option
    'v1.6 now calculated by annual time steps
    'v1.7 now corporate with Database function, read/write are using the function in database interface
    'now all file related functions are using databaseinterface

    Dim RoadCapArray(145, 7) As String
    Dim ZoneInput As String
    Dim ZoneDetails() As String
    Dim ZoneID As Long
    Dim BaseVkm(145, 1) As Double
    Dim ZonePop(145, 1) As Double
    Dim ZoneGVA(145, 1) As Double
    Dim ZoneSpeed(145, 1) As Double
    Dim ZoneCarCost(145, 1), ZoneLGVCost(145, 1), ZoneHGV1Cost(145, 1), ZoneHGV2Cost(145, 1), ZonePSVCost(145, 1) As Double
    Dim ZoneExtVar(145, 45) As String
    Dim YearCount As Integer
    Dim NewVkm As Double
    Dim ZoneOutputRow As String
    Dim ZoneLaneKm(145, 4) As Double
    Dim ZoneSpdNew As Double
    Dim RdZoneEl(90, 9) As String
    Dim RoadCatProb(4) As Double
    Dim VClass As String
    Dim FuelSpeed As Double
    Dim BaseSpeed(145, 4) As Double
    Dim FuelPerKm As Double
    Dim PetrolUsed As Double
    Dim DieselUsed As Double
    Dim ElectricUsed, LPGUsed, CNGUsed, HydrogenUsed As Double
    Dim BaseRVCatTraf(145, 4, 5), RVCatTraf(4, 5), BaseRoadCatTraffic(145, 4), RoadCatTraffic(145, 4) As Double
    Dim VehTypeSplit(145, 4, 5) As Double
    Dim RoadCatKm(145, 4) As Double
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim PopRat(5), GVARat(5), SpdRat, PetRat(5), VkmRat(5) As Double
    Dim NewLaneKm(4) As Double
    Dim SuppressedTraffic(4, 5) As Double
    Dim BaseCatSpeed(145, 4), BaseCatB(145, 4), BaseCatC(145, 4), NewCatSpeed(145, 4) As Double
    Dim SpeedC, SpeedB, SpeedA As Double
    Dim Constrained(4) As Boolean
    Dim Latentvkm(145, 4) As Double
    Dim StratLine As String
    Dim StratArray(90, 95) As String
    Dim AddedLaneKm(145, 4) As Double
    Dim RdTripRates(1) As Double
    Dim VKmVType(10) As Double
    Dim BuiltLaneKm(145, 4) As Double
    Dim ZoneLine As String
    Dim ZoneArray() As String
    Dim OutputRow As String
    Dim InputArray(144, 33) As String
    Dim OutputArray(144, 27) As String
    Dim TempArray(144, 74) As String
    Dim CapNum As Integer
    Dim NewCapNum As Integer
    Dim FuelArray(145, 19) As String
    Dim NewCapArray(145, 5) As String




    Public Sub RoadZoneMainNew()

        'read related files
        Call ZoneSetFiles()

        'set year counter to the entered value
        YearCount = StartYear

        Do Until YearCount > StartYear + Duration

            'initialize new cap num to write to the first line
            NewCapNum = 1

            'get external variable values
            Call ReadData("RoadZone", "ExtVar", ZoneExtVar, modelRunID, , YearCount)

            'read from the initial file if it is year 2010
            If YearCount = 1 Then
                Call ReadData("RoadZone", "Input", InputArray, True)
            Else
                ReDim Preserve InputArray(144, 74)
                Call ReadData("RoadZone", "Input", InputArray, False)
            End If

            ZoneID = 1

            Do Until ZoneID > 144

                'update the input variables
                Call LoadZoneInput()

                'apply zone equation to adjust demand
                Call RoadZoneKm()

                'estimate fuel consumption
                Call RoadZoneFuelConsumption()

                'write output array and temp array
                Call WriteRoadZoneOutput()

                ZoneID += 1
            Loop

            'create file is true if it is the initial year and write to outputfile and temp file
            If YearCount = StartYear Then
                Call WriteData("RoadZone", "Output", OutputArray, TempArray, True)
                Call WriteData("RoadZone", "RoadZoneFuel", FuelArray, , True)
                If BuildInfra = True Then
                    Call WriteData("RoadZone", "RoadZoneNewCap", NewCapArray, , True)
                End If
            Else
                Call WriteData("RoadZone", "Output", OutputArray, TempArray, False)
                Call WriteData("RoadZone", "RoadZoneFuel", FuelArray, , False)
                If BuildInfra = True Then
                    Call WriteData("RoadZone", "RoadZoneNewCap", NewCapArray, , False)
                End If
            End If

            'move on to next year
            YearCount += 1
        Loop


    End Sub

    Public Sub ZoneSetFiles()
        'This sub selects the input data files

        If UpdateExtVars = True Then
            If NewRdZCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If

        'read in elasticity array
        Call ReadData("RoadZone", "Elasticity", RdZoneEl, modelRunID)

        '130514 note that at the moment this just reads a blank file - need to remove "fileprefix" from string, and then need to modify form1 and build procedure so that it picks up on additional capacity built (ie those tagged '-1' for year)
        Call ReadData("RoadZone", "CapChange", RoadCapArray, modelRunID)
        CapNum = 1
        If RoadCapArray(CapNum, 0) Is Nothing Then
            '130514 addition of line
            RoadCapArray(CapNum, 0) = 0
            RoadCapArray(CapNum, 1) = -1
            RoadCapArray(CapNum, 2) = ""
        End If

        'get the strategy file
        Call ReadData("SubStrategy", "", StratArray, modelRunID)


    End Sub

    Sub LoadZoneInput()
        Dim SumProbKm As Double

        'read the input data for the inputarray which is from the database
        If YearCount = 1 Then
            'read the input data for the zone

            BaseVkm(ZoneID, 1) = InputArray(ZoneID, 2)
            ZonePop(ZoneID, 1) = InputArray(ZoneID, 3)
            ZoneGVA(ZoneID, 1) = InputArray(ZoneID, 4)
            ZoneSpeed(ZoneID, 1) = InputArray(ZoneID, 5)
            ZoneCarCost(ZoneID, 1) = InputArray(ZoneID, 6)
            ZoneLGVCost(ZoneID, 1) = InputArray(ZoneID, 18)
            ZoneHGV1Cost(ZoneID, 1) = InputArray(ZoneID, 19)
            ZoneHGV2Cost(ZoneID, 1) = InputArray(ZoneID, 20)
            ZonePSVCost(ZoneID, 1) = InputArray(ZoneID, 21)
            ZoneLaneKm(ZoneID, 1) = InputArray(ZoneID, 8)
            ZoneLaneKm(ZoneID, 2) = CDbl(InputArray(ZoneID, 9)) + CDbl(InputArray(ZoneID, 10))
            ZoneLaneKm(ZoneID, 3) = InputArray(ZoneID, 11)
            ZoneLaneKm(ZoneID, 4) = CDbl(InputArray(ZoneID, 12)) + CDbl(InputArray(ZoneID, 13))
            RoadCatProb(1) = InputArray(ZoneID, 14)
            RoadCatProb(2) = InputArray(ZoneID, 15)
            RoadCatProb(3) = InputArray(ZoneID, 16)
            RoadCatProb(4) = InputArray(ZoneID, 17)
            'allocate the vkm to vehicle types and road types based on the initial proportions and on the proportion of traffic on different road types
            'convert lane km to road km for each road category, where Cat1 is motorways, Cat2 is rural A, Cat3 is rural minor and Cat4 is urban
            RoadCatKm(ZoneID, 1) = InputArray(ZoneID, 8) / 6
            RoadCatKm(ZoneID, 2) = (CDbl(InputArray(ZoneID, 9)) / 4) + (CDbl(InputArray(ZoneID, 10)) / 2)
            RoadCatKm(ZoneID, 3) = InputArray(ZoneID, 11) / 2
            RoadCatKm(ZoneID, 4) = (CDbl(InputArray(ZoneID, 12)) / 4) + (CDbl(InputArray(ZoneID, 13)) / 2)
            'v1.4 mod - this now comes straight from input file
            BaseRoadCatTraffic(ZoneID, 1) = InputArray(ZoneID, 22)
            BaseRoadCatTraffic(ZoneID, 2) = InputArray(ZoneID, 23)
            BaseRoadCatTraffic(ZoneID, 3) = InputArray(ZoneID, 24)
            BaseRoadCatTraffic(ZoneID, 4) = InputArray(ZoneID, 25)
            'the proportions come from DfT Traffic Statistics Table TRA0204 - see model guide for more details
            '**if there is a sudden decline in traffic then we probably just want to multiply by rcprob rather than by rcprob*rck/sumpkm
            'v1.4 mod - calculation of these now much simpler
            SumProbKm = (RoadCatProb(1) * RoadCatKm(ZoneID, 1)) + (RoadCatProb(2) * RoadCatKm(ZoneID, 2)) + (RoadCatProb(3) * RoadCatKm(ZoneID, 3)) + (RoadCatProb(4) * RoadCatKm(ZoneID, 4))
            BaseRVCatTraf(ZoneID, 1, 1) = BaseRoadCatTraffic(ZoneID, 1) * 0.755
            BaseRVCatTraf(ZoneID, 2, 1) = BaseRoadCatTraffic(ZoneID, 2) * 0.791
            BaseRVCatTraf(ZoneID, 3, 1) = BaseRoadCatTraffic(ZoneID, 3) * 0.794
            BaseRVCatTraf(ZoneID, 4, 1) = BaseRoadCatTraffic(ZoneID, 4) * 0.829
            BaseRVCatTraf(ZoneID, 1, 2) = BaseRoadCatTraffic(ZoneID, 1) * 0.123
            BaseRVCatTraf(ZoneID, 2, 2) = BaseRoadCatTraffic(ZoneID, 2) * 0.135
            BaseRVCatTraf(ZoneID, 3, 2) = BaseRoadCatTraffic(ZoneID, 3) * 0.174
            BaseRVCatTraf(ZoneID, 4, 2) = BaseRoadCatTraffic(ZoneID, 4) * 0.132
            BaseRVCatTraf(ZoneID, 1, 3) = BaseRoadCatTraffic(ZoneID, 1) * 0.037
            BaseRVCatTraf(ZoneID, 2, 3) = BaseRoadCatTraffic(ZoneID, 2) * 0.03
            BaseRVCatTraf(ZoneID, 3, 3) = BaseRoadCatTraffic(ZoneID, 3) * 0.019
            BaseRVCatTraf(ZoneID, 4, 3) = BaseRoadCatTraffic(ZoneID, 4) * 0.016
            BaseRVCatTraf(ZoneID, 1, 4) = BaseRoadCatTraffic(ZoneID, 1) * 0.081
            BaseRVCatTraf(ZoneID, 2, 4) = BaseRoadCatTraffic(ZoneID, 2) * 0.038
            BaseRVCatTraf(ZoneID, 3, 4) = BaseRoadCatTraffic(ZoneID, 3) * 0.005
            BaseRVCatTraf(ZoneID, 4, 4) = BaseRoadCatTraffic(ZoneID, 4) * 0.006
            BaseRVCatTraf(ZoneID, 1, 5) = BaseRoadCatTraffic(ZoneID, 1) * 0.004
            BaseRVCatTraf(ZoneID, 2, 5) = BaseRoadCatTraffic(ZoneID, 2) * 0.006
            BaseRVCatTraf(ZoneID, 3, 5) = BaseRoadCatTraffic(ZoneID, 3) * 0.009
            BaseRVCatTraf(ZoneID, 4, 5) = BaseRoadCatTraffic(ZoneID, 4) * 0.017

            'v1.4 mod - need to get the base speed for each road category and VkmB and VkmC from input file
            BaseCatSpeed(ZoneID, 1) = InputArray(ZoneID, 26)
            BaseCatSpeed(ZoneID, 2) = InputArray(ZoneID, 27)
            BaseCatSpeed(ZoneID, 3) = InputArray(ZoneID, 28)
            BaseCatSpeed(ZoneID, 4) = InputArray(ZoneID, 29)
            For x = 1 To 4
                BaseSpeed(ZoneID, x) = BaseCatSpeed(ZoneID, x)
            Next
            BaseCatB(ZoneID, 1) = InputArray(ZoneID, 22)
            BaseCatB(ZoneID, 2) = InputArray(ZoneID, 23)
            BaseCatB(ZoneID, 3) = InputArray(ZoneID, 24)
            BaseCatB(ZoneID, 4) = InputArray(ZoneID, 25)
            BaseCatC(ZoneID, 1) = InputArray(ZoneID, 30)
            BaseCatC(ZoneID, 2) = InputArray(ZoneID, 31)
            BaseCatC(ZoneID, 3) = InputArray(ZoneID, 32)
            BaseCatC(ZoneID, 4) = InputArray(ZoneID, 33)

            'v1.4 modification - reset latent and constrained values 
            For x = 1 To 4
                Constrained(x) = False
                Latentvkm(ZoneID, x) = 0
                '130514 - need to reset this anyway
                'If BuildInfra = True Then
                AddedLaneKm(ZoneID, x) = 0
                'End If
                BuiltLaneKm(ZoneID, x) = 0
            Next
        Else
            ZonePop(ZoneID, 1) = InputArray(ZoneID, 2)
            ZoneGVA(ZoneID, 1) = InputArray(ZoneID, 3)
            ZoneSpeed(ZoneID, 1) = CDbl(InputArray(ZoneID, 4))
            ZoneCarCost(ZoneID, 1) = CDbl(InputArray(ZoneID, 5))
            ZoneLGVCost(ZoneID, 1) = CDbl(InputArray(ZoneID, 6))
            ZoneHGV1Cost(ZoneID, 1) = CDbl(InputArray(ZoneID, 7))
            ZoneHGV2Cost(ZoneID, 1) = CDbl(InputArray(ZoneID, 8))
            ZonePSVCost(ZoneID, 1) = CDbl(InputArray(ZoneID, 9))
            BaseVkm(ZoneID, 1) = CDbl(InputArray(ZoneID, 10))
            ZoneLaneKm(ZoneID, 1) = CDbl(InputArray(ZoneID, 11))
            ZoneLaneKm(ZoneID, 2) = CDbl(InputArray(ZoneID, 12))
            ZoneLaneKm(ZoneID, 3) = CDbl(InputArray(ZoneID, 13))
            ZoneLaneKm(ZoneID, 4) = CDbl(InputArray(ZoneID, 14))
            RoadCatKm(ZoneID, 1) = CDbl(InputArray(ZoneID, 15))
            RoadCatKm(ZoneID, 2) = CDbl(InputArray(ZoneID, 16))
            RoadCatKm(ZoneID, 3) = CDbl(InputArray(ZoneID, 17))
            RoadCatKm(ZoneID, 4) = CDbl(InputArray(ZoneID, 18))

            'loop through 4 road type (motorway, rural, ruralmin, urban)
            For x = 1 To 4
                BaseRoadCatTraffic(ZoneID, x) = CDbl(InputArray(ZoneID, 19 + 6 * (x - 1)))
                BaseCatSpeed(ZoneID, x) = CDbl(InputArray(ZoneID, 59 + (x - 1)))
                For y = 1 To 5
                    BaseRVCatTraf(ZoneID, x, y) = CDbl(InputArray(ZoneID, 19 + 6 * (x - 1) + y))
                Next
            Next
            'add back the suppressed traffic if using smarter choices
            If SmarterChoices = True Then
                BaseRVCatTraf(ZoneID, 4, 1) += CDbl(InputArray(ZoneID, 55))
                BaseRoadCatTraffic(ZoneID, 4) += CDbl(InputArray(ZoneID, 55))
                'SuppressedTraffic(4, 1) = 0
            End If

            If UrbanFrt = True Then
                For y = 2 To 4
                    BaseRVCatTraf(ZoneID, 4, y) += CDbl(InputArray(ZoneID, 54 + y))
                    BaseRoadCatTraffic(ZoneID, 4) += CDbl(InputArray(ZoneID, 54 + y))
                    'SuppressedTraffic(4, y) = 0
                Next
            End If

            If SmartFrt = True Then
                For x = 1 To 3
                    For y = 3 To 4
                        BaseRVCatTraf(ZoneID, x, y) += CDbl(InputArray(ZoneID, 43 + 4 * (x - 1) + (y - 1)))
                        BaseRoadCatTraffic(ZoneID, x) += CDbl(InputArray(ZoneID, 43 + 4 * (x - 1) + (y - 1)))
                        'SuppressedTraffic(x, y) = 0
                    Next
                Next
            End If
            For x = 1 To 4
                Latentvkm(ZoneID, x) = InputArray(ZoneID, 62 + x)
            Next

            For x = 1 To 4
                AddedLaneKm(ZoneID, x) = InputArray(ZoneID, 66 + x)
            Next

            For x = 1 To 4
                BuiltLaneKm(ZoneID, x) = InputArray(ZoneID, 70 + x)
            Next

        End If

    End Sub


    Sub RoadZoneKm()
        'v1.3 now calculate traffic separately for the different road types (this is to allow the fuel consumption calculations to work with changes in fuel mix and vehicle mix over time)
        Dim rdtype As Integer
        Dim iteratecount As Long
        Dim starttraffic As Double

        'v1.4 mod get the trip rates
        If TripRates = "Strategy" Then
            RdTripRates(0) = StratArray(YearCount, 91)
            RdTripRates(1) = StratArray(YearCount, 92)
        End If

        'now incorporates variable elasticities - only do this here if we are not using them - otherwise do it in a separate sub
        If VariableEl = False Then
            'Calculate the values of the various input ratios for the different types of road vehicle (speed assumed to be the same for all)
            If TripRates = "Strategy" Then
                PopRat(1) = ((ZoneExtVar(ZoneID, 2) * RdTripRates(0)) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 1)
                PopRat(2) = ((ZoneExtVar(ZoneID, 2) * RdTripRates(1)) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
                PopRat(3) = ((ZoneExtVar(ZoneID, 2) * RdTripRates(1)) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
                PopRat(4) = ((ZoneExtVar(ZoneID, 2) * RdTripRates(1)) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
                PopRat(5) = ((ZoneExtVar(ZoneID, 2) * RdTripRates(0)) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 1)
            Else
                PopRat(1) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 1)
                PopRat(2) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
                PopRat(3) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
                PopRat(4) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
                PopRat(5) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 1)
            End If
            PopRat(1) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 1)
            PopRat(2) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
            PopRat(3) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
            PopRat(4) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 6)
            PopRat(5) = (ZoneExtVar(ZoneID, 2) / ZonePop(ZoneID, 1)) ^ RdZoneEl(YearCount, 1)
            GVARat(1) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 2)
            GVARat(2) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 7)
            GVARat(3) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 7)
            GVARat(4) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 7)
            GVARat(5) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 2)
            SpdRat = (ZoneSpeed(ZoneID, 1) / ZoneSpeed(ZoneID, 1)) ^ RdZoneEl(YearCount, 3)
            'calculate the ratio for different types of road vehicle
            PetRat(1) = (ZoneExtVar(ZoneID, 4) / ZoneCarCost(ZoneID, 1)) ^ RdZoneEl(YearCount, 4)
            PetRat(2) = (ZoneExtVar(ZoneID, 42) / ZoneLGVCost(ZoneID, 1)) ^ RdZoneEl(YearCount, 9)
            PetRat(3) = (ZoneExtVar(ZoneID, 43) / ZoneHGV1Cost(ZoneID, 1)) ^ RdZoneEl(YearCount, 9)
            PetRat(4) = (ZoneExtVar(ZoneID, 44) / ZoneHGV2Cost(ZoneID, 1)) ^ RdZoneEl(YearCount, 9)
            PetRat(5) = (ZoneExtVar(ZoneID, 45) / ZonePSVCost(ZoneID, 1)) ^ RdZoneEl(YearCount, 4)

            'Combine these ratios to get the vkm ratios
            For x = 1 To 5
                VkmRat(x) = PopRat(x) * GVARat(x) * SpdRat * PetRat(x)
            Next
        End If

        'v1.4 mod - check if there is any change in road capacity in this zone and year
        '130514 modified to take in both prespecified capacity and capacity built as part of TR1
        If RoadCapArray(CapNum, 0) = ZoneID Then
            If RoadCapArray(CapNum, 1) = YearCount Then
                AddedLaneKm(ZoneID, 1) += RoadCapArray(CapNum, 2)
                AddedLaneKm(ZoneID, 2) += CDbl(RoadCapArray(CapNum, 3)) + RoadCapArray(CapNum, 4)
                AddedLaneKm(ZoneID, 3) += RoadCapArray(CapNum, 5)
                AddedLaneKm(ZoneID, 4) += CDbl(RoadCapArray(CapNum, 6)) + RoadCapArray(CapNum, 7)
                CapNum += 1
            End If
        End If
        If BuildInfra = True Then
            For a = 1 To 4
                AddedLaneKm(ZoneID, a) += BuiltLaneKm(ZoneID, a)
            Next
        End If

        NewLaneKm(1) = ZoneExtVar(ZoneID, 6) + AddedLaneKm(ZoneID, 1)
        NewLaneKm(2) = CDbl(ZoneExtVar(ZoneID, 7)) + CDbl(ZoneExtVar(ZoneID, 8)) + AddedLaneKm(ZoneID, 2)
        NewLaneKm(3) = ZoneExtVar(ZoneID, 9) + AddedLaneKm(ZoneID, 3)
        NewLaneKm(4) = CDbl(ZoneExtVar(ZoneID, 10)) + CDbl(ZoneExtVar(ZoneID, 11)) + AddedLaneKm(ZoneID, 4)

        For x = 1 To 4
            If NewLaneKm(x) <> ZoneLaneKm(ZoneID, x) Then
                BaseCatB(ZoneID, x) = BaseCatB(ZoneID, x) * (NewLaneKm(x) / ZoneLaneKm(ZoneID, x))
                BaseCatC(ZoneID, x) = BaseCatC(ZoneID, x) * (NewLaneKm(x) / ZoneLaneKm(ZoneID, x))
                BaseRoadCatTraffic(ZoneID, x) += Latentvkm(ZoneID, x)
                Latentvkm(ZoneID, x) = 0
                For y = 1 To 5
                    BaseRVCatTraf(ZoneID, x, y) = BaseRoadCatTraffic(ZoneID, x) * VehTypeSplit(ZoneID, x, y)
                Next
            End If
        Next

        'update the maximum capacities based on the strategy files
        BaseCatB(ZoneID, 1) = BaseCatB(ZoneID, 1) * StratArray(YearCount, 87)
        BaseCatB(ZoneID, 2) = BaseCatB(ZoneID, 2) * StratArray(YearCount, 88)
        BaseCatB(ZoneID, 3) = BaseCatB(ZoneID, 3) * StratArray(YearCount, 89)
        BaseCatB(ZoneID, 4) = BaseCatB(ZoneID, 4) * StratArray(YearCount, 90)
        BaseCatC(ZoneID, 1) = BaseCatC(ZoneID, 1) * StratArray(YearCount, 87)
        BaseCatC(ZoneID, 2) = BaseCatC(ZoneID, 2) * StratArray(YearCount, 88)
        BaseCatC(ZoneID, 3) = BaseCatC(ZoneID, 3) * StratArray(YearCount, 89)
        BaseCatC(ZoneID, 4) = BaseCatC(ZoneID, 4) * StratArray(YearCount, 90)

        'loop through the four road types calculating the traffic levels
        For rdtype = 1 To 4
            'check if there is any of that road type in the zone
            If NewLaneKm(rdtype) > 0 Then

                'Multiply the vkm ratio by the previous year's vkm to get new vkm figures
                If VariableEl = True Then
                    'if using variable elasticities then calculate these in a separate sub - if not then they were calculated at the start of this sub
                    OldX = NewLaneKm(rdtype)
                    Call GetVariableElasticities()
                End If
                For x = 1 To 5
                    RVCatTraf(rdtype, x) = BaseRVCatTraf(ZoneID, rdtype, x) * VkmRat(x)
                Next

                RoadCatTraffic(ZoneID, rdtype) = RVCatTraf(rdtype, 1) + RVCatTraf(rdtype, 2) + RVCatTraf(rdtype, 3) + RVCatTraf(rdtype, 4) + RVCatTraf(rdtype, 5)

                'v1.3 mod - if using smarter choices, smart logistics or urban freight innovations we only set the vehicle type splits in the first year, to avoid car traffic share declining away to nothing
                If SmarterChoices = True Then
                    If YearCount = 1 Then
                        'set the vehicle type splits
                        For x = 1 To 5
                            VehTypeSplit(ZoneID, rdtype, x) = RVCatTraf(rdtype, x) / RoadCatTraffic(ZoneID, rdtype)
                        Next
                    End If
                ElseIf UrbanFrt = True Then
                    If YearCount = 1 Then
                        'set the vehicle type splits
                        For x = 1 To 5
                            VehTypeSplit(ZoneID, rdtype, x) = RVCatTraf(rdtype, x) / RoadCatTraffic(ZoneID, rdtype)
                        Next
                    End If
                ElseIf SmartFrt = True Then
                    If YearCount = 1 Then
                        'set the vehicle type splits
                        For x = 1 To 5
                            VehTypeSplit(ZoneID, rdtype, x) = RVCatTraf(rdtype, x) / RoadCatTraffic(ZoneID, rdtype)
                        Next
                    End If
                Else
                    'set the vehicle type splits
                    For x = 1 To 5
                        VehTypeSplit(ZoneID, rdtype, x) = RVCatTraf(rdtype, x) / RoadCatTraffic(ZoneID, rdtype)
                    Next
                End If
                'Set up new road km variable from external variables and calculate resulting change in speed
                'v1.4 this calculation changed to use speed-flow curve

                Select Case rdtype
                    Case 1
                        SpeedA = 71.95
                        SpeedB = 69.96
                        SpeedC = 34.55
                    Case 2
                        SpeedA = 56.05
                        SpeedB = 50.14
                        SpeedC = 27.22
                    Case 3
                        SpeedA = 42.56
                        SpeedB = 39.77
                        SpeedC = 24.85
                    Case 4
                        SpeedA = 22.24
                        SpeedB = 13.5
                        SpeedC = 9.07
                End Select

                If RoadCatTraffic(ZoneID, rdtype) < BaseCatB(ZoneID, rdtype) Then
                    'if traffic is less than the base point B level then new speed between point A and point B
                    NewCatSpeed(ZoneID, rdtype) = ((RoadCatTraffic(ZoneID, rdtype) / BaseCatB(ZoneID, rdtype)) * (SpeedB - SpeedA)) + SpeedA
                    SpdRat = NewCatSpeed(ZoneID, rdtype) / BaseCatSpeed(ZoneID, rdtype)
                ElseIf RoadCatTraffic(ZoneID, rdtype) <= BaseCatC(ZoneID, rdtype) Then
                    'otherwise if traffic is between base point C level then new speed is between point B and point C
                    NewCatSpeed(ZoneID, rdtype) = (((RoadCatTraffic(ZoneID, rdtype) - BaseCatB(ZoneID, rdtype)) / (BaseCatC(ZoneID, rdtype) - BaseCatB(ZoneID, rdtype))) * (SpeedC - SpeedB)) + SpeedB
                    SpdRat = NewCatSpeed(ZoneID, rdtype) / BaseCatSpeed(ZoneID, rdtype)
                Else
                    'if traffic is greater than point C level then need to apply constraint
                    Latentvkm(ZoneID, rdtype) += (RoadCatTraffic(ZoneID, rdtype) - BaseCatC(ZoneID, rdtype))
                    RoadCatTraffic(ZoneID, rdtype) = BaseCatC(ZoneID, rdtype)
                    NewCatSpeed(ZoneID, rdtype) = SpeedC
                    SpdRat = 1
                End If

                '1.4 modification to catch iteration that fails to converge
                iteratecount = 0
                starttraffic = RoadCatTraffic(ZoneID, rdtype)

                'iterate between calculation of speed and vkm ratios unti convergence reached
                Do Until SpdRat >= 0.999 And SpdRat <= 1.001
                    'set the base vkm to equal the previous new vkm
                    BaseRoadCatTraffic(ZoneID, rdtype) = RoadCatTraffic(ZoneID, rdtype)
                    'recalculate the vehicle km figure
                    'now includes variable elasticity option
                    If VariableEl = True Then
                        OldX = BaseRoadCatTraffic(ZoneID, rdtype)
                        OldY = BaseCatSpeed(ZoneID, rdtype)
                        NewY = NewCatSpeed(ZoneID, rdtype)
                        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                            OldEl = RdZoneEl(YearCount, 3)
                            Call VarElCalc()
                            SpdRat = VarRat
                        Else
                            SpdRat = (NewCatSpeed(ZoneID, rdtype) / BaseCatSpeed(ZoneID, rdtype)) ^ RdZoneEl(YearCount, 3)
                        End If
                    Else
                        SpdRat = (NewCatSpeed(ZoneID, rdtype) / BaseCatSpeed(ZoneID, rdtype)) ^ RdZoneEl(YearCount, 3)
                    End If
                    RoadCatTraffic(ZoneID, rdtype) = SpdRat * BaseRoadCatTraffic(ZoneID, rdtype)
                    'set the base speed to equal the previous new speed
                    BaseCatSpeed(ZoneID, rdtype) = NewCatSpeed(ZoneID, rdtype)
                    'calculate the resulting change in speed from the new vehicle km figure
                    If RoadCatTraffic(ZoneID, rdtype) < BaseCatB(ZoneID, rdtype) Then
                        'if traffic is less than the base point B level then new speed between point A and point B
                        NewCatSpeed(ZoneID, rdtype) = ((RoadCatTraffic(ZoneID, rdtype) / BaseCatB(ZoneID, rdtype)) * (SpeedB - SpeedA)) + SpeedA
                    ElseIf RoadCatTraffic(ZoneID, rdtype) <= BaseCatC(ZoneID, rdtype) Then
                        'otherwise it is between point B and point C
                        NewCatSpeed(ZoneID, rdtype) = (((RoadCatTraffic(ZoneID, rdtype) - BaseCatB(ZoneID, rdtype)) / (BaseCatC(ZoneID, rdtype) - BaseCatB(ZoneID, rdtype))) * (SpeedC - SpeedB)) + SpeedB
                    Else
                        Latentvkm(ZoneID, rdtype) += (RoadCatTraffic(ZoneID, rdtype) - BaseCatC(ZoneID, rdtype))
                        RoadCatTraffic(ZoneID, rdtype) = BaseCatC(ZoneID, rdtype)
                        NewCatSpeed(ZoneID, rdtype) = SpeedC
                        SpdRat = 1
                        Exit Do
                    End If
                    'SpdRat = NewCatSpeed(zoneid,ZoneID,rdtype) / BaseCatSpeed(rdtype)
                    'v1.4 modification to catch iterations that fail to converge
                    iteratecount += 1
                    If iteratecount = 1000 Then
                        RoadCatTraffic(ZoneID, rdtype) = starttraffic
                        If RoadCatTraffic(ZoneID, rdtype) < BaseCatB(ZoneID, rdtype) Then
                            'if traffic is less than the base point B level then new speed between point A and point B
                            NewCatSpeed(ZoneID, rdtype) = ((RoadCatTraffic(ZoneID, rdtype) / BaseCatB(ZoneID, rdtype)) * (SpeedB - SpeedA)) + SpeedA
                        Else
                            'otherwise it is between point B and point C
                            NewCatSpeed(ZoneID, rdtype) = (((RoadCatTraffic(ZoneID, rdtype) - BaseCatB(ZoneID, rdtype)) / (BaseCatC(ZoneID, rdtype) - BaseCatB(ZoneID, rdtype))) * (SpeedC - SpeedB)) + SpeedB
                        End If
                        Exit Do
                    End If
                Loop

                'split the final vkm figure between vehicle types
                For x = 1 To 5
                    RVCatTraf(rdtype, x) = RoadCatTraffic(ZoneID, rdtype) * VehTypeSplit(ZoneID, rdtype, x)
                Next

            Else
                RoadCatTraffic(ZoneID, rdtype) = 0
                For x = 1 To 5
                    RVCatTraf(rdtype, x) = 0
                Next
            End If
        Next

        If RoadCatTraffic(ZoneID, 4) > 0 Then
            'v1.3 if using smarter choices then scale the urban car traffic accordingly
            If SmarterChoices = True Then
                'check if we are after the date of introduction
                If SmartIntro < YearCount Then
                    'if so then subtract the unscaled urban car traffic from the total urban traffic
                    'need to store the suppressed traffic, as otherwise the model will keep suppressing demand by the set % each year, leading to a much greater cumulative decay than anticipated
                    RoadCatTraffic(ZoneID, 4) = RoadCatTraffic(ZoneID, 4) - RVCatTraf(4, 1)
                    'then check if we are less than the number of years to take full effect after the date of introduction
                    If (SmartIntro + SmartYears) > YearCount Then
                        SuppressedTraffic(4, 1) = RVCatTraf(4, 1) * (SmartPer * ((YearCount - SmartIntro) / SmartYears))
                        RVCatTraf(4, 1) = RVCatTraf(4, 1) * (1 - (SmartPer * ((YearCount - SmartIntro) / SmartYears)))
                    Else
                        'otherwise just scale by the full amount
                        SuppressedTraffic(4, 1) = RVCatTraf(4, 1) * SmartPer
                        RVCatTraf(4, 1) = RVCatTraf(4, 1) * (1 - SmartPer)
                    End If
                    'add the scaled car traffic back on to the rest of the urban traffic
                    RoadCatTraffic(ZoneID, 4) += RVCatTraf(4, 1)
                    'v1.4 recalculate speed
                    SpeedA = 22.24
                    SpeedB = 13.5
                    SpeedC = 9.07
                    If RoadCatTraffic(ZoneID, 4) < BaseCatB(ZoneID, 4) Then
                        'if traffic is less than the base point B level then new speed between point A and point B
                        NewCatSpeed(ZoneID, 4) = ((RoadCatTraffic(ZoneID, 4) / BaseCatB(ZoneID, 4)) * (SpeedB - SpeedA)) + SpeedA
                    Else
                        'otherwise it is between point B and point C
                        NewCatSpeed(ZoneID, 4) = (((RoadCatTraffic(ZoneID, 4) - BaseCatB(ZoneID, 4)) / (BaseCatC(ZoneID, 4) - BaseCatB(ZoneID, 4))) * (SpeedC - SpeedB)) + SpeedB
                    End If
                End If
            End If

            'v1.3 if using urban freight innovations then scale the urban LGV/HGV traffic accordingly
            If UrbanFrt = True Then
                'check if we are after the date of introduction
                If UrbFrtIntro < YearCount Then
                    'if so then subtract the unscaled urban LGV/HGV traffic from the total urban traffic
                    'need to store the suppressed traffic, as otherwise the model will keep suppressing demand by the set % each year, leading to a much greater cumulative decay than anticipated
                    RoadCatTraffic(ZoneID, 4) = RoadCatTraffic(ZoneID, 4) - (RVCatTraf(4, 2) + RVCatTraf(4, 3) + RVCatTraf(4, 4))
                    'then check if we are less than the number of years to take full effect after the date of introduction
                    If (UrbFrtIntro + UrbFrtYears) > YearCount Then
                        SuppressedTraffic(4, 2) = RVCatTraf(4, 2) * (UrbFrtPer * ((YearCount - UrbFrtIntro) / UrbFrtYears))
                        SuppressedTraffic(4, 3) = RVCatTraf(4, 3) * (UrbFrtPer * ((YearCount - UrbFrtIntro) / UrbFrtYears))
                        SuppressedTraffic(4, 4) = RVCatTraf(4, 4) * (UrbFrtPer * ((YearCount - UrbFrtIntro) / UrbFrtYears))
                        RVCatTraf(4, 2) = RVCatTraf(4, 2) * (1 - (UrbFrtPer * ((YearCount - UrbFrtIntro) / UrbFrtYears)))
                        RVCatTraf(4, 3) = RVCatTraf(4, 3) * (1 - (UrbFrtPer * ((YearCount - UrbFrtIntro) / UrbFrtYears)))
                        RVCatTraf(4, 4) = RVCatTraf(4, 4) * (1 - (UrbFrtPer * ((YearCount - UrbFrtIntro) / UrbFrtYears)))
                    Else
                        'otherwise just scale by the full amount
                        SuppressedTraffic(4, 2) = RVCatTraf(4, 2) * UrbFrtPer
                        SuppressedTraffic(4, 3) = RVCatTraf(4, 3) * UrbFrtPer
                        SuppressedTraffic(4, 4) = RVCatTraf(4, 4) * UrbFrtPer
                        RVCatTraf(4, 2) = RVCatTraf(4, 2) * (1 - UrbFrtPer)
                        RVCatTraf(4, 3) = RVCatTraf(4, 3) * (1 - UrbFrtPer)
                        RVCatTraf(4, 4) = RVCatTraf(4, 4) * (1 - UrbFrtPer)
                    End If
                    'add the scaled freight traffic back on to the rest of the urban traffic
                    RoadCatTraffic(ZoneID, 4) = RoadCatTraffic(ZoneID, 4) + RVCatTraf(4, 2) + RVCatTraf(4, 3) + RVCatTraf(4, 4)
                    'v1.4 recalculate speed
                    SpeedA = 22.24
                    SpeedB = 13.5
                    SpeedC = 9.07
                    If RoadCatTraffic(ZoneID, 4) < BaseCatB(ZoneID, 4) Then
                        'if traffic is less than the base point B level then new speed between point A and point B
                        NewCatSpeed(ZoneID, 4) = ((RoadCatTraffic(ZoneID, 4) / BaseCatB(ZoneID, 4)) * (SpeedB - SpeedA)) + SpeedA
                    Else
                        'otherwise it is between point B and point C
                        NewCatSpeed(ZoneID, 4) = (((RoadCatTraffic(ZoneID, 4) - BaseCatB(ZoneID, 4)) / (BaseCatC(ZoneID, 4) - BaseCatB(ZoneID, 4))) * (SpeedC - SpeedB)) + SpeedB
                    End If
                End If
            End If
        End If


        'v1.3 if using smart logistics then scale the non-urban HGV traffic accordingly
        If SmartFrt = True Then
            'check if we are after the date of introduction
            If SmFrtIntro < YearCount Then
                For x = 1 To 3
                    If RoadCatTraffic(ZoneID, x) > 0 Then
                        'if so then subtract the unscaled non-urban HGV traffic from the total urban traffic
                        'need to store the suppressed traffic, as otherwise the model will keep suppressing demand by the set % each year, leading to a much greater cumulative decay than anticipated
                        RoadCatTraffic(ZoneID, x) = RoadCatTraffic(ZoneID, x) - (RVCatTraf(x, 3) + RVCatTraf(x, 4))
                        'then check if we are less than the number of years to take full effect after the date of introduction
                        If (SmFrtIntro + SmFrtYears) > YearCount Then
                            For y = 3 To 4
                                SuppressedTraffic(x, y) = RVCatTraf(x, y) * (SmFrtPer * ((YearCount - SmFrtIntro) / SmFrtYears))
                                RVCatTraf(x, y) = RVCatTraf(x, y) * (1 - (SmFrtPer * ((YearCount - SmFrtIntro) / SmFrtYears)))
                            Next
                        Else
                            'otherwise just scale by the full amount
                            For y = 3 To 4
                                SuppressedTraffic(x, y) = RVCatTraf(x, y) * SmFrtPer
                                RVCatTraf(x, y) = RVCatTraf(x, y) * (1 - SmFrtPer)
                            Next
                        End If
                        'add the scaled freight traffic back on to the rest of the road traffic
                        RoadCatTraffic(ZoneID, x) = RoadCatTraffic(ZoneID, x) + RVCatTraf(x, 3) + RVCatTraf(x, 4)
                        'v1.4 recalculate speed
                        Select Case x
                            Case 1
                                SpeedA = 71.95
                                SpeedB = 69.96
                                SpeedC = 34.55
                            Case 2
                                SpeedA = 56.05
                                SpeedB = 50.14
                                SpeedC = 27.22
                            Case 3
                                SpeedA = 42.56
                                SpeedB = 39.77
                                SpeedC = 24.85
                            Case 4
                                SpeedA = 22.24
                                SpeedB = 13.5
                                SpeedC = 9.07
                        End Select
                        If RoadCatTraffic(ZoneID, x) < BaseCatB(ZoneID, x) Then
                            'if traffic is less than the base point B level then new speed between point A and point B
                            NewCatSpeed(ZoneID, x) = ((RoadCatTraffic(ZoneID, x) / BaseCatB(ZoneID, x)) * (SpeedB - SpeedA)) + SpeedA
                        Else
                            'otherwise it is between point B and point C
                            NewCatSpeed(ZoneID, x) = (((RoadCatTraffic(ZoneID, x) - BaseCatB(ZoneID, x)) / (BaseCatC(ZoneID, x) - BaseCatB(ZoneID, x))) * (SpeedC - SpeedB)) + SpeedB
                        End If
                    End If
                Next
            End If
        End If


        'calculate the total vkm figure and average speed
        NewVkm = RoadCatTraffic(ZoneID, 1) + RoadCatTraffic(ZoneID, 2) + RoadCatTraffic(ZoneID, 3) + RoadCatTraffic(ZoneID, 4)
        ZoneSpdNew = ((RoadCatTraffic(ZoneID, 1) * NewCatSpeed(ZoneID, 1)) + (RoadCatTraffic(ZoneID, 2) * NewCatSpeed(ZoneID, 2)) + (RoadCatTraffic(ZoneID, 3) * NewCatSpeed(ZoneID, 3)) + (RoadCatTraffic(ZoneID, 4) * NewCatSpeed(ZoneID, 4))) / NewVkm

    End Sub

    Sub GetVariableElasticities()
        'Calculate the values of the various input ratios for the different types of road vehicle (speed assumed to be the same for all)

        'pop1ratio
        OldY = ZonePop(ZoneID, 1)
        If TripRates = "Strategy" Then
            NewY = ZoneExtVar(ZoneID, 2) * RdTripRates(0)
        Else
            NewY = ZoneExtVar(ZoneID, 2)
        End If
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 1)
            Call VarElCalc()
            PopRat(1) = VarRat
        Else
            PopRat(1) = (NewY / OldY) ^ RdZoneEl(YearCount, 1)
        End If
        'pop2ratio
        OldY = ZonePop(ZoneID, 1)
        If TripRates = "Strategy" Then
            NewY = ZoneExtVar(ZoneID, 2) * RdTripRates(1)
        Else
            NewY = ZoneExtVar(ZoneID, 2)
        End If
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 6)
            Call VarElCalc()
            PopRat(2) = VarRat
        Else
            PopRat(2) = (NewY / OldY) ^ RdZoneEl(YearCount, 6)
        End If
        'pop3ratio
        OldY = ZonePop(ZoneID, 1)
        If TripRates = "Strategy" Then
            NewY = ZoneExtVar(ZoneID, 2) * RdTripRates(1)
        Else
            NewY = ZoneExtVar(ZoneID, 2)
        End If
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 6)
            Call VarElCalc()
            PopRat(3) = VarRat
        Else
            PopRat(3) = (NewY / OldY) ^ RdZoneEl(YearCount, 6)
        End If
        'pop4ratio
        OldY = ZonePop(ZoneID, 1)
        If TripRates = "Strategy" Then
            NewY = ZoneExtVar(ZoneID, 2) * RdTripRates(1)
        Else
            NewY = ZoneExtVar(ZoneID, 2)
        End If
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 6)
            Call VarElCalc()
            PopRat(4) = VarRat
        Else
            PopRat(4) = (NewY / OldY) ^ RdZoneEl(YearCount, 6)
        End If
        'pop5ratio
        OldY = ZonePop(ZoneID, 1)
        If TripRates = "Strategy" Then
            NewY = ZoneExtVar(ZoneID, 2) * RdTripRates(0)
        Else
            NewY = ZoneExtVar(ZoneID, 2)
        End If
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 1)
            Call VarElCalc()
            PopRat(5) = VarRat
        Else
            PopRat(5) = (NewY / OldY) ^ RdZoneEl(YearCount, 1)
        End If
        'gva1ratio
        OldY = ZoneGVA(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 3)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 2)
            Call VarElCalc()
            GVARat(1) = VarRat
        Else
            GVARat(1) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 2)
        End If
        'gva2ratio
        OldY = ZoneGVA(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 3)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 7)
            Call VarElCalc()
            GVARat(2) = VarRat
        Else
            GVARat(2) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 7)
        End If
        'gva3ratio
        OldY = ZoneGVA(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 3)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 7)
            Call VarElCalc()
            GVARat(3) = VarRat
        Else
            GVARat(3) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 7)
        End If
        'gva4ratio
        OldY = ZoneGVA(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 3)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 7)
            Call VarElCalc()
            GVARat(4) = VarRat
        Else
            GVARat(4) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 7)
        End If
        'gva5ratio
        OldY = ZoneGVA(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 3)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 7)
            Call VarElCalc()
            GVARat(5) = VarRat
        Else
            GVARat(5) = (ZoneExtVar(ZoneID, 3) / ZoneGVA(ZoneID, 1)) ^ RdZoneEl(YearCount, 2)
        End If
        'speed ratio - this is constant
        SpdRat = (ZoneSpeed(ZoneID, 1) / ZoneSpeed(ZoneID, 1)) ^ RdZoneEl(YearCount, 3)
        'cost1ratio
        OldY = ZoneCarCost(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 4)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 4)
            Call VarElCalc()
            PetRat(1) = VarRat
        Else
            PetRat(1) = (ZoneExtVar(ZoneID, 4) / ZoneCarCost(ZoneID, 1)) ^ RdZoneEl(YearCount, 4)
        End If
        'cost2ratio
        OldY = ZoneLGVCost(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 42)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 9)
            Call VarElCalc()
            PetRat(2) = VarRat
        Else
            PetRat(2) = (ZoneExtVar(ZoneID, 42) / ZoneLGVCost(ZoneID, 1)) ^ RdZoneEl(YearCount, 9)
        End If
        'cost3ratio
        OldY = ZoneHGV1Cost(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 43)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 9)
            Call VarElCalc()
            PetRat(3) = VarRat
        Else
            PetRat(3) = (ZoneExtVar(ZoneID, 43) / ZoneHGV1Cost(ZoneID, 1)) ^ RdZoneEl(YearCount, 9)
        End If
        'cost4ratio
        OldY = ZoneHGV2Cost(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 44)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 9)
            Call VarElCalc()
            PetRat(4) = VarRat
        Else
            PetRat(4) = (ZoneExtVar(ZoneID, 44) / ZoneHGV2Cost(ZoneID, 1)) ^ RdZoneEl(YearCount, 9)
        End If
        'cost5ratio
        OldY = ZonePSVCost(ZoneID, 1)
        NewY = ZoneExtVar(ZoneID, 45)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(YearCount, 4)
            Call VarElCalc()
            PetRat(5) = VarRat
        Else
            PetRat(5) = (ZoneExtVar(ZoneID, 45) / ZonePSVCost(ZoneID, 1)) ^ RdZoneEl(YearCount, 4)
        End If
        'Combine these ratios to get the vkm ratios
        For x = 1 To 5
            VkmRat(x) = PopRat(x) * GVARat(x) * SpdRat * PetRat(x)
        Next


    End Sub

    Sub VarElCalc()
        Dim alpha, beta As Double
        Dim xnew As Double

        alpha = OldX / Math.Exp(OldEl)
        beta = (Math.Log(OldX / alpha)) / OldY
        xnew = alpha * Math.Exp(beta * NewY)
        VarRat = xnew / OldX

    End Sub

    Sub RoadZoneFuelConsumption()
        'estimates fuel consumption for the zone and year in question
        'note that this has now been modified to use five vehicle types (with HGVs split), not four as previously
        '1.3 note that this has been further modified to include ten fuel types -and also to take into account changes in vehicle fuel efficiency
        '1.4 now modified to use road type specific speeds rather than average zonal speeds
        Dim CatCount As Integer
        Dim RVFCatTraf(4, 5, 10) As Double
        Dim RVFFuel(4, 5, 10) As Double
        Dim VCount As Integer
        Dim CatFuelTotal As Double

        'split the vehicle km by vehicle category data into fuel types, where fuel 1 is petrol, fuel 2 is diesel, fuel 3 is petrol hybrid, fuel 4 is diesel hybrid, fuel 5 is plug-in hybrid,
        'fuel 6 is battery electric, fuel 7 is LPG, fuel 8 is CNG, fuel 9 is hydrogen ICE and fuel 10 is hydrogen fuel cell
        'note that for PHEVs urban roads will use electricity, whereas other roads will use petrol/diesel
        For CatCount = 1 To 4
            RVFCatTraf(CatCount, 1, 1) = RVCatTraf(CatCount, 1) * ZoneExtVar(ZoneID, 12)
            RVFCatTraf(CatCount, 1, 2) = RVCatTraf(CatCount, 1) * ZoneExtVar(ZoneID, 13)
            RVFCatTraf(CatCount, 1, 3) = RVCatTraf(CatCount, 1) * ZoneExtVar(ZoneID, 25)
            RVFCatTraf(CatCount, 1, 4) = RVCatTraf(CatCount, 1) * ZoneExtVar(ZoneID, 26)
            RVFCatTraf(CatCount, 1, 5) = RVCatTraf(CatCount, 1) * ZoneExtVar(ZoneID, 27)
            RVFCatTraf(CatCount, 1, 6) = RVCatTraf(CatCount, 1) * ZoneExtVar(ZoneID, 14)
            RVFCatTraf(CatCount, 1, 9) = RVCatTraf(CatCount, 1) * ZoneExtVar(ZoneID, 28)
            RVFCatTraf(CatCount, 1, 10) = RVCatTraf(CatCount, 1) * ZoneExtVar(ZoneID, 29)
            RVFCatTraf(CatCount, 2, 1) = RVCatTraf(CatCount, 2) * ZoneExtVar(ZoneID, 15)
            RVFCatTraf(CatCount, 2, 2) = RVCatTraf(CatCount, 2) * ZoneExtVar(ZoneID, 16)
            RVFCatTraf(CatCount, 2, 4) = RVCatTraf(CatCount, 2) * ZoneExtVar(ZoneID, 30)
            RVFCatTraf(CatCount, 2, 5) = RVCatTraf(CatCount, 2) * ZoneExtVar(ZoneID, 31)
            RVFCatTraf(CatCount, 2, 6) = RVCatTraf(CatCount, 2) * ZoneExtVar(ZoneID, 17)
            RVFCatTraf(CatCount, 2, 7) = RVCatTraf(CatCount, 2) * ZoneExtVar(ZoneID, 32)
            RVFCatTraf(CatCount, 2, 8) = RVCatTraf(CatCount, 2) * ZoneExtVar(ZoneID, 33)
            RVFCatTraf(CatCount, 3, 2) = RVCatTraf(CatCount, 3) * ZoneExtVar(ZoneID, 18)
            RVFCatTraf(CatCount, 3, 4) = RVCatTraf(CatCount, 3) * ZoneExtVar(ZoneID, 39)
            RVFCatTraf(CatCount, 3, 9) = RVCatTraf(CatCount, 3) * ZoneExtVar(ZoneID, 40)
            RVFCatTraf(CatCount, 3, 10) = RVCatTraf(CatCount, 3) * ZoneExtVar(ZoneID, 41)
            RVFCatTraf(CatCount, 4, 2) = RVCatTraf(CatCount, 4) * ZoneExtVar(ZoneID, 18)
            RVFCatTraf(CatCount, 4, 4) = RVCatTraf(CatCount, 4) * ZoneExtVar(ZoneID, 39)
            RVFCatTraf(CatCount, 4, 9) = RVCatTraf(CatCount, 4) * ZoneExtVar(ZoneID, 40)
            RVFCatTraf(CatCount, 4, 10) = RVCatTraf(CatCount, 4) * ZoneExtVar(ZoneID, 41)
            RVFCatTraf(CatCount, 5, 2) = RVCatTraf(CatCount, 5) * ZoneExtVar(ZoneID, 20)
            RVFCatTraf(CatCount, 5, 4) = RVCatTraf(CatCount, 5) * ZoneExtVar(ZoneID, 34)
            RVFCatTraf(CatCount, 5, 5) = RVCatTraf(CatCount, 5) * ZoneExtVar(ZoneID, 35)
            RVFCatTraf(CatCount, 5, 6) = RVCatTraf(CatCount, 5) * ZoneExtVar(ZoneID, 21)
            RVFCatTraf(CatCount, 5, 7) = RVCatTraf(CatCount, 5) * ZoneExtVar(ZoneID, 36)
            RVFCatTraf(CatCount, 5, 8) = RVCatTraf(CatCount, 5) * ZoneExtVar(ZoneID, 37)
            RVFCatTraf(CatCount, 5, 10) = RVCatTraf(CatCount, 5) * ZoneExtVar(ZoneID, 38)
        Next

        'estimate fuel consumption for each vehicle type
        'initial average speeds taken from tables in model description document - but this will need to change year on year to reflect changes in congestion
        'Petrol cars
        VClass = "CarP"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 1) = RVFCatTraf(1, 1, 1) * FuelPerKm * StratArray(YearCount, 31)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (75.639 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 1) = RVFCatTraf(2, 1, 1) * FuelPerKm * StratArray(YearCount, 31)
        Else
            RVFFuel(2, 1, 1) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 1) = RVFCatTraf(3, 1, 1) * FuelPerKm * StratArray(YearCount, 31)
        'urban
        FuelSpeed = 52.143 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 1, 1) = RVFCatTraf(4, 1, 1) * FuelPerKm * StratArray(YearCount, 31)
        'Diesel cars
        VClass = "CarD"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 2) = RVFCatTraf(1, 1, 2) * FuelPerKm * StratArray(YearCount, 32)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (75.639 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 2) = RVFCatTraf(2, 1, 2) * FuelPerKm * StratArray(YearCount, 32)
        Else
            RVFFuel(2, 1, 2) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 2) = RVFCatTraf(3, 1, 2) * FuelPerKm * StratArray(YearCount, 32)
        'urban
        FuelSpeed = 52.143 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 1, 2) = RVFCatTraf(4, 1, 2) * FuelPerKm * StratArray(YearCount, 32)
        'Petrol hybrid cars - these are being calculated based on a proportional adjustment of the petrol fuel consumption figures (ie dividing the Brand hybrid figure by the Brand petrol figure and then multiplying by the DfT petrol figure)
        VClass = "CarP"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 3) = RVFCatTraf(1, 1, 3) * (FuelPerKm * (11.2 / 18.6)) * StratArray(YearCount, 43)
        'rural a
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (75.639 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 3) = RVFCatTraf(2, 1, 3) * (FuelPerKm * (11.2 / 18.6)) * StratArray(YearCount, 43)
        Else
            RVFFuel(2, 1, 3) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 3) = RVFCatTraf(3, 1, 3) * (FuelPerKm * (11.2 / 18.6)) * StratArray(YearCount, 43)
        'urban
        FuelSpeed = 52.143 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 1, 3) = RVFCatTraf(4, 1, 3) * (FuelPerKm * (11.2 / 18.6)) * StratArray(YearCount, 43)
        'Diesel hybrid cars  - these are being calculated based on a proportional adjustment of the diesel fuel consumption figures (ie dividing the Brand hybrid figure by the Brand diesel figure and then multiplying by the DfT diesel figure)
        VClass = "CarD"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 4) = RVFCatTraf(1, 1, 4) * (FuelPerKm * (7.5 / 12.4)) * StratArray(YearCount, 44)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (75.639 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 4) = RVFCatTraf(2, 1, 4) * (FuelPerKm * (7.5 / 12.4)) * StratArray(YearCount, 44)
        Else
            RVFFuel(2, 1, 4) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 4) = RVFCatTraf(3, 1, 4) * (FuelPerKm * (7.5 / 12.4)) * StratArray(YearCount, 44)
        'urban
        FuelSpeed = 52.143 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 1, 4) = RVFCatTraf(4, 1, 4) * (FuelPerKm * (7.5 / 12.4)) * StratArray(YearCount, 44)
        'Plug-in hybrid cars - for rural driving these use a proportional adjustment of the Brand figures (petrol/diesel), whereas for urban driving they use the Brand electric figures
        VClass = "CarP"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 5) = RVFCatTraf(1, 1, 5) * (FuelPerKm * (18.1 / 25.9)) * StratArray(YearCount, 45)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (75.639 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 5) = RVFCatTraf(2, 1, 5) * (FuelPerKm * (18.1 / 25.9)) * StratArray(YearCount, 45)
        Else
            RVFFuel(2, 1, 5) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 5) = RVFCatTraf(3, 1, 5) * (FuelPerKm * (18.1 / 25.9)) * StratArray(YearCount, 45)
        'urban
        RVFFuel(4, 1, 5) = RVFCatTraf(4, 1, 5) * 0.1557 * StratArray(YearCount, 45)
        'Battery electric cars - fuel consumption figure now taken from Brand (2010)
        'motorway
        RVFFuel(1, 1, 6) = RVFCatTraf(1, 1, 6) * 0.165 * StratArray(YearCount, 33)
        'rural a
        RVFFuel(2, 1, 6) = RVFCatTraf(2, 1, 6) * 0.165 * StratArray(YearCount, 33)
        'rural minor
        RVFFuel(3, 1, 6) = RVFCatTraf(3, 1, 6) * 0.165 * StratArray(YearCount, 33)
        'urban
        RVFFuel(4, 1, 6) = RVFCatTraf(4, 1, 6) * 0.165 * StratArray(YearCount, 33)
        'hydrogen ICE cars - fuel consumption figure from Brand (2010)
        'motorway
        RVFFuel(1, 1, 9) = RVFCatTraf(1, 1, 9) * 0.438 * StratArray(YearCount, 46)
        'rural a
        RVFFuel(2, 1, 9) = RVFCatTraf(2, 1, 9) * 0.438 * StratArray(YearCount, 46)
        'rural minor
        RVFFuel(3, 1, 9) = RVFCatTraf(3, 1, 9) * 0.438 * StratArray(YearCount, 46)
        'urban
        RVFFuel(4, 1, 9) = RVFCatTraf(4, 1, 9) * 0.438 * StratArray(YearCount, 46)
        'hydrogen fuel cell cars - fuel consumption figure from Brand (2010)
        'motorway
        RVFFuel(1, 1, 10) = RVFCatTraf(1, 1, 10) * 0.1777 * StratArray(YearCount, 47)
        'rural a
        RVFFuel(2, 1, 10) = RVFCatTraf(2, 1, 10) * 0.1777 * StratArray(YearCount, 47)
        'rural minor
        RVFFuel(3, 1, 10) = RVFCatTraf(3, 1, 10) * 0.1777 * StratArray(YearCount, 47)
        'urban
        RVFFuel(4, 1, 10) = RVFCatTraf(4, 1, 10) * 0.1777 * StratArray(YearCount, 47)

        'Petrol LGVs
        VClass = "LGVP"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 2, 1) = RVFCatTraf(1, 2, 1) * FuelPerKm * StratArray(YearCount, 34)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 2) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (77.249 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 2, 1) = RVFCatTraf(2, 2, 1) * FuelPerKm * StratArray(YearCount, 34)
        Else
            RVFFuel(2, 2, 1) = 0
        End If
        'rural minor
        FuelSpeed = 77.249 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 2, 1) = RVFCatTraf(3, 2, 1) * FuelPerKm * StratArray(YearCount, 34)
        'urban
        FuelSpeed = 52.786 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 2, 1) = RVFCatTraf(4, 2, 1) * FuelPerKm * StratArray(YearCount, 34)
        'Diesel LGVs
        VClass = "LGVD"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 2, 2) = RVFCatTraf(1, 2, 2) * FuelPerKm * StratArray(YearCount, 35)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 2) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (77.249 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 2, 2) = RVFCatTraf(2, 2, 2) * FuelPerKm * StratArray(YearCount, 35)
        Else
            RVFFuel(2, 2, 2) = 0
        End If
        'rural minor
        FuelSpeed = 77.249 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 2, 2) = RVFCatTraf(3, 2, 2) * FuelPerKm * StratArray(YearCount, 35)
        'urban
        FuelSpeed = 52.786 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 2, 2) = RVFCatTraf(4, 2, 2) * FuelPerKm * StratArray(YearCount, 35)
        'diesel hybrid LGVs
        VClass = "LGVD"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 2, 4) = RVFCatTraf(1, 2, 4) * (FuelPerKm * (4.4 / 7.9)) * StratArray(YearCount, 48)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 2) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (77.249 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 2, 4) = RVFCatTraf(2, 2, 4) * (FuelPerKm * (4.4 / 7.9)) * StratArray(YearCount, 48)
        Else
            RVFFuel(2, 2, 4) = 0
        End If
        'rural minor
        FuelSpeed = 77.249 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 2, 4) = RVFCatTraf(3, 2, 4) * (FuelPerKm * (4.4 / 7.9)) * StratArray(YearCount, 48)
        'urban
        FuelSpeed = 52.786 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 2, 4) = RVFCatTraf(4, 2, 4) * (FuelPerKm * (4.4 / 7.9)) * StratArray(YearCount, 48)
        'plug-in hybrid LGVs - for rural driving these use a proportional adjustment of the Brand figures (petrol/diesel), whereas for urban driving they use the Brand electric figures
        VClass = "LGVD"
        'motorway
        FuelSpeed = 111.04 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 2, 5) = RVFCatTraf(1, 2, 5) * (FuelPerKm * (5.8 / 7.9)) * StratArray(YearCount, 49)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 2) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(ZoneID, 7)) + (77.249 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 2, 5) = RVFCatTraf(2, 2, 5) * (FuelPerKm * (5.8 / 7.9)) * StratArray(YearCount, 49)
        Else
            RVFFuel(2, 2, 5) = 0
        End If
        'rural minor
        FuelSpeed = 77.249 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 2, 5) = RVFCatTraf(3, 2, 5) * (FuelPerKm * (5.8 / 7.9)) * StratArray(YearCount, 49)
        'urban
        RVFFuel(4, 2, 5) = RVFCatTraf(4, 2, 5) * 0.423 * StratArray(YearCount, 49)
        'Battery Electric LGVs - fuel consumption figure now from Brand (2010)
        'motorway
        RVFFuel(1, 2, 6) = RVFCatTraf(1, 2, 6) * 0.562 * StratArray(YearCount, 36)
        'rural a
        RVFFuel(2, 2, 6) = RVFCatTraf(2, 2, 6) * 0.562 * StratArray(YearCount, 36)
        'rural minor
        RVFFuel(3, 2, 6) = RVFCatTraf(3, 2, 6) * 0.562 * StratArray(YearCount, 36)
        'urban
        RVFFuel(4, 2, 6) = RVFCatTraf(4, 2, 6) * 0.562 * StratArray(YearCount, 36)
        'LPG LGVs
        'motorway
        RVFFuel(1, 2, 7) = RVFCatTraf(1, 2, 7) * 0.118 * StratArray(YearCount, 50)
        'rural a
        RVFFuel(2, 2, 7) = RVFCatTraf(2, 2, 7) * 0.118 * StratArray(YearCount, 50)
        'rural minor
        RVFFuel(3, 2, 7) = RVFCatTraf(3, 2, 7) * 0.118 * StratArray(YearCount, 50)
        'urban
        RVFFuel(4, 2, 7) = RVFCatTraf(4, 2, 7) * 0.118 * StratArray(YearCount, 50)
        'CNG LGVs
        'motorway
        RVFFuel(1, 2, 8) = RVFCatTraf(1, 2, 8) * 0.808 * StratArray(YearCount, 51)
        'rural a
        RVFFuel(2, 2, 8) = RVFCatTraf(2, 2, 8) * 0.808 * StratArray(YearCount, 51)
        'rural minor
        RVFFuel(3, 2, 8) = RVFCatTraf(3, 2, 8) * 0.808 * StratArray(YearCount, 51)
        'urban
        RVFFuel(4, 2, 8) = RVFCatTraf(4, 2, 8) * 0.808 * StratArray(YearCount, 51)

        'Diesel  2-3 axle rigid HGVs
        VClass = "HGV1D"
        'motorway
        FuelSpeed = 92.537 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 3, 2) = RVFCatTraf(1, 3, 2) * FuelPerKm * StratArray(YearCount, 37)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 3) > 0 Then
            FuelSpeed = (((90.928 * ZoneExtVar(ZoneID, 7)) + (70.811 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 3, 2) = RVFCatTraf(2, 3, 2) * FuelPerKm * StratArray(YearCount, 37)
        Else
            RVFFuel(2, 3, 2) = 0
        End If
        'rural minor
        FuelSpeed = 70.811 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 3, 2) = RVFCatTraf(3, 3, 2) * FuelPerKm * StratArray(YearCount, 37)
        'urban
        FuelSpeed = 51.579 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 3, 2) = RVFCatTraf(4, 3, 2) * FuelPerKm * StratArray(YearCount, 37)
        'diesel hybrid small HGVs
        VClass = "HGV1D"
        'motorway
        FuelSpeed = 92.537 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 3, 4) = RVFCatTraf(1, 3, 4) * (FuelPerKm * (15 / 25.9)) * StratArray(YearCount, 57)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 3) > 0 Then
            FuelSpeed = (((90.928 * ZoneExtVar(ZoneID, 7)) + (70.811 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 3, 4) = RVFCatTraf(2, 3, 4) * (FuelPerKm * (15 / 25.9)) * StratArray(YearCount, 57)
        Else
            RVFFuel(2, 3, 4) = 0
        End If
        'rural minor
        FuelSpeed = 70.811 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 3, 4) = RVFCatTraf(3, 3, 4) * (FuelPerKm * (15 / 25.9)) * StratArray(YearCount, 57)
        'urban
        FuelSpeed = 51.579 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 3, 4) = RVFCatTraf(4, 3, 4) * (FuelPerKm * (15 / 25.9)) * StratArray(YearCount, 57)
        'hydrogen ICE small HGVs
        'motorway
        RVFFuel(1, 3, 9) = RVFCatTraf(1, 3, 9) * 0.957 * StratArray(YearCount, 58)
        'rural a
        RVFFuel(2, 3, 9) = RVFCatTraf(2, 3, 9) * 0.957 * StratArray(YearCount, 58)
        'rural minor
        RVFFuel(3, 3, 9) = RVFCatTraf(3, 3, 9) * 0.957 * StratArray(YearCount, 58)
        'urban
        RVFFuel(4, 3, 9) = RVFCatTraf(4, 3, 9) * 0.957 * StratArray(YearCount, 58)
        'hydrogen fuel cell small HGVs
        'motorway
        RVFFuel(1, 3, 10) = RVFCatTraf(1, 3, 10) * 0.898 * StratArray(YearCount, 59)
        'rural a
        RVFFuel(2, 3, 10) = RVFCatTraf(2, 3, 10) * 0.898 * StratArray(YearCount, 59)
        'rural minor
        RVFFuel(3, 3, 10) = RVFCatTraf(3, 3, 10) * 0.898 * StratArray(YearCount, 59)
        'urban
        RVFFuel(4, 3, 10) = RVFCatTraf(4, 3, 10) * 0.898 * StratArray(YearCount, 59)

        'Diesel 4+ axle rigid and artic HGVs
        VClass = "HGV2D"
        'motorway
        FuelSpeed = 86.905 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 4, 2) = RVFCatTraf(1, 4, 2) * FuelPerKm * StratArray(YearCount, 39)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 4) > 0 Then
            FuelSpeed = (((85.295 * ZoneExtVar(ZoneID, 7)) + (69.685 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 4, 2) = RVFCatTraf(2, 4, 2) * FuelPerKm * StratArray(YearCount, 39)
        Else
            RVFFuel(2, 4, 2) = 0
        End If
        'rural minor
        FuelSpeed = 69.685 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 4, 2) = RVFCatTraf(3, 4, 2) * FuelPerKm * StratArray(YearCount, 39)
        'urban
        FuelSpeed = 53.511 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 4, 2) = RVFCatTraf(4, 4, 2) * FuelPerKm * StratArray(YearCount, 39)
        'diesel hybrid large HGVs
        VClass = "HGV2D"
        'motorway
        FuelSpeed = 86.905 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 4, 4) = RVFCatTraf(1, 4, 4) * (FuelPerKm * (22.1 / 37.6)) * StratArray(YearCount, 60)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 4) > 0 Then
            FuelSpeed = (((85.295 * ZoneExtVar(ZoneID, 7)) + (69.685 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 4, 4) = RVFCatTraf(2, 4, 4) * (FuelPerKm * (22.1 / 37.6)) * StratArray(YearCount, 60)
        Else
            RVFFuel(2, 4, 4) = 0
        End If
        'rural minor
        FuelSpeed = 69.685 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 4, 4) = RVFCatTraf(3, 4, 4) * (FuelPerKm * (22.1 / 37.6)) * StratArray(YearCount, 60)
        'urban
        FuelSpeed = 53.511 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 4, 4) = RVFCatTraf(4, 4, 4) * (FuelPerKm * (22.1 / 37.6)) * StratArray(YearCount, 60)
        'hydrogen ICE large HGVs
        'motorway
        RVFFuel(1, 4, 9) = RVFCatTraf(1, 4, 9) * 1.398 * StratArray(YearCount, 61)
        'rural a
        RVFFuel(2, 4, 9) = RVFCatTraf(2, 4, 9) * 1.398 * StratArray(YearCount, 61)
        'rural minor
        RVFFuel(3, 4, 9) = RVFCatTraf(3, 4, 9) * 1.398 * StratArray(YearCount, 61)
        'urban
        RVFFuel(4, 4, 9) = RVFCatTraf(4, 4, 9) * 1.398 * StratArray(YearCount, 61)
        'hydrogen fuel cell large HGVs
        'motorway
        RVFFuel(1, 4, 10) = RVFCatTraf(1, 4, 10) * 1.123 * StratArray(YearCount, 62)
        'rural a
        RVFFuel(2, 4, 10) = RVFCatTraf(2, 4, 10) * 1.123 * StratArray(YearCount, 62)
        'rural minor
        RVFFuel(3, 4, 10) = RVFCatTraf(3, 4, 10) * 1.123 * StratArray(YearCount, 62)
        'urban
        RVFFuel(4, 4, 10) = RVFCatTraf(4, 4, 10) * 1.123 * StratArray(YearCount, 62)

        'Diesel PSVs
        VClass = "PSVD"
        'motorway
        FuelSpeed = 98.17 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 5, 2) = RVFCatTraf(1, 5, 2) * FuelPerKm * StratArray(YearCount, 41)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 5) > 0 Then
            FuelSpeed = (((96.561 * ZoneExtVar(ZoneID, 7)) + (72.42 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 5, 2) = RVFCatTraf(2, 5, 2) * FuelPerKm * StratArray(YearCount, 41)
        Else
            RVFFuel(2, 5, 2) = 0
        End If
        'rural minor
        FuelSpeed = 72.42 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 5, 2) = RVFCatTraf(3, 5, 2) * FuelPerKm * StratArray(YearCount, 41)
        'urban
        FuelSpeed = 48.924 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 5, 2) = RVFCatTraf(4, 5, 2) * FuelPerKm * StratArray(YearCount, 41)
        'Diesel hybrid PSVs
        VClass = "PSVD"
        'motorway
        FuelSpeed = 98.17 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 5, 4) = RVFCatTraf(1, 5, 4) * (FuelPerKm * (18.5 / 17.6)) * StratArray(YearCount, 52)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 5) > 0 Then
            FuelSpeed = (((96.561 * ZoneExtVar(ZoneID, 7)) + (72.42 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 5, 4) = RVFCatTraf(2, 5, 4) * (FuelPerKm * (11.9 / 19.6)) * StratArray(YearCount, 52)
        Else
            RVFFuel(2, 5, 4) = 0
        End If
        'rural minor
        FuelSpeed = 72.42 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 5, 4) = RVFCatTraf(3, 5, 4) * (FuelPerKm * (11.9 / 19.6)) * StratArray(YearCount, 52)
        'urban
        FuelSpeed = 48.924 * (NewCatSpeed(ZoneID, 4) / BaseSpeed(ZoneID, 4))
        Call VehicleFuelConsumption()
        RVFFuel(4, 5, 4) = RVFCatTraf(4, 5, 4) * (FuelPerKm * (11.9 / 19.6)) * StratArray(YearCount, 52)
        'Plug-in hybrid PSVs
        VClass = "PSVD"
        'motorway
        FuelSpeed = 98.17 * (NewCatSpeed(ZoneID, 1) / BaseSpeed(ZoneID, 1))
        Call VehicleFuelConsumption()
        RVFFuel(1, 5, 5) = RVFCatTraf(1, 5, 5) * (FuelPerKm * (11.9 / 19.6)) * StratArray(YearCount, 53)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 5) > 0 Then
            FuelSpeed = (((96.561 * ZoneExtVar(ZoneID, 7)) + (72.42 * ZoneExtVar(ZoneID, 8))) / (CDbl(ZoneExtVar(ZoneID, 7)) + ZoneExtVar(ZoneID, 8))) * (NewCatSpeed(ZoneID, 2) / BaseSpeed(ZoneID, 2))
            Call VehicleFuelConsumption()
            RVFFuel(2, 5, 5) = RVFCatTraf(2, 5, 5) * (FuelPerKm * (11.9 / 19.6)) * StratArray(YearCount, 53)
        Else
            RVFFuel(2, 5, 5) = 0
        End If
        'rural minor
        FuelSpeed = 72.42 * (NewCatSpeed(ZoneID, 3) / BaseSpeed(ZoneID, 3))
        Call VehicleFuelConsumption()
        RVFFuel(3, 5, 5) = RVFCatTraf(3, 5, 5) * (FuelPerKm * (11.9 / 19.6)) * StratArray(YearCount, 53)
        'urban
        RVFFuel(4, 5, 5) = RVFCatTraf(4, 5, 5) * 1.037 * StratArray(YearCount, 53)
        '***need to alter battery electric PSVs
        'Battery Electric PSVs - electricity consumption figure now from Brand (2010)
        'motorway
        RVFFuel(1, 5, 6) = RVFCatTraf(1, 5, 6) * 1.7 * StratArray(YearCount, 42)
        'rural a
        RVFFuel(2, 5, 6) = RVFCatTraf(2, 5, 6) * 1.7 * StratArray(YearCount, 42)
        'rural minor
        RVFFuel(3, 5, 6) = RVFCatTraf(3, 5, 6) * 1.7 * StratArray(YearCount, 42)
        'urban
        RVFFuel(4, 5, 6) = RVFCatTraf(4, 5, 6) * 1.7 * StratArray(YearCount, 42)
        'LPG PSVs
        'motorway
        RVFFuel(1, 5, 7) = RVFCatTraf(1, 5, 7) * 0.954 * StratArray(YearCount, 54)
        'rural a
        RVFFuel(2, 5, 7) = RVFCatTraf(2, 5, 7) * 0.364 * StratArray(YearCount, 54)
        'rural minor
        RVFFuel(3, 5, 7) = RVFCatTraf(3, 5, 7) * 0.364 * StratArray(YearCount, 54)
        'urban
        RVFFuel(4, 5, 7) = RVFCatTraf(4, 5, 7) * 0.364 * StratArray(YearCount, 54)
        'CNG PSVs
        'motorway
        RVFFuel(1, 5, 8) = RVFCatTraf(1, 5, 8) * 3.749 * StratArray(YearCount, 55)
        'rural a
        RVFFuel(2, 5, 8) = RVFCatTraf(2, 5, 8) * 6.283 * StratArray(YearCount, 55)
        'rural minor
        RVFFuel(3, 5, 8) = RVFCatTraf(3, 5, 8) * 6.283 * StratArray(YearCount, 55)
        'urban
        RVFFuel(4, 5, 8) = RVFCatTraf(4, 5, 8) * 6.283 * StratArray(YearCount, 55)
        'Hydrogen fuel cell PSVs
        'motorway
        RVFFuel(1, 5, 10) = RVFCatTraf(1, 5, 10) * 0.546 * StratArray(YearCount, 56)
        'rural a
        RVFFuel(2, 5, 10) = RVFCatTraf(2, 5, 10) * 0.546 * StratArray(YearCount, 56)
        'rural minor
        RVFFuel(3, 5, 10) = RVFCatTraf(3, 5, 10) * 0.546 * StratArray(YearCount, 56)
        'urban
        RVFFuel(4, 5, 10) = RVFCatTraf(4, 5, 10) * 0.546 * StratArray(YearCount, 56)

        'sum the total amount of each fuel used
        'petrol, now includes petrol hybrids
        For CatCount = 1 To 4
            For VCount = 1 To 2
                PetrolUsed += RVFFuel(CatCount, VCount, 1)
            Next
            PetrolUsed += RVFFuel(CatCount, 1, 3)
            If CatCount < 4 Then
                PetrolUsed += RVFFuel(CatCount, 1, 5)
            End If
        Next
        'scaling factor to bring forecasts into line with observed figures
        PetrolUsed = PetrolUsed * 1.19888732
        'diesel, now includes diesel hybrids
        For CatCount = 1 To 4
            For VCount = 1 To 5
                DieselUsed += RVFFuel(CatCount, VCount, 2)
                DieselUsed += RVFFuel(CatCount, VCount, 4)
                If CatCount < 4 Then
                    DieselUsed += RVFFuel(CatCount, VCount, 5)
                End If
            Next
        Next
        'scaling factor to bring forecasts into line with observed figures
        DieselUsed = DieselUsed * 1.12752146
        'electricity, now includes plug in hybrids on urban roads
        For CatCount = 1 To 4
            For VCount = 1 To 5
                ElectricUsed += RVFFuel(CatCount, VCount, 6)
                If CatCount = 4 Then
                    ElectricUsed += RVFFuel(CatCount, VCount, 5)
                End If
            Next
        Next
        'LPG
        For CatCount = 1 To 4
            For VCount = 2 To 5 Step 3
                LPGUsed += RVFFuel(CatCount, VCount, 7)
            Next
        Next
        'CNG
        For CatCount = 1 To 4
            For VCount = 2 To 5 Step 3
                CNGUsed += RVFFuel(CatCount, VCount, 8)
            Next
        Next
        'hydrogen
        For CatCount = 1 To 4
            For VCount = 1 To 5
                HydrogenUsed += RVFFuel(CatCount, VCount, 9)
                HydrogenUsed += RVFFuel(CatCount, VCount, 10)
            Next
        Next

        'Mod - now works out vkm by fuel type
        For v = 1 To 10
            VKmVType(v) = 0
        Next
        For c = 1 To 4
            'petrol
            VKmVType(1) += (RVFCatTraf(c, 1, 1) + RVFCatTraf(c, 2, 1))
            'diesel
            VKmVType(2) += (RVFCatTraf(c, 1, 2) + RVFCatTraf(c, 2, 2) + RVFCatTraf(c, 3, 2) + RVFCatTraf(c, 4, 2) + RVFCatTraf(c, 5, 2))
            'petrol hybrid
            VKmVType(3) += RVFCatTraf(c, 1, 3)
            'diesel hybrid
            VKmVType(4) += (RVFCatTraf(c, 1, 4) + RVFCatTraf(c, 2, 4) + RVFCatTraf(c, 3, 4) + RVFCatTraf(c, 4, 4) + RVFCatTraf(c, 5, 4))
            'plug-in hybrid,
            VKmVType(5) += (RVFCatTraf(c, 1, 5) + RVFCatTraf(c, 2, 5) + RVFCatTraf(c, 5, 5))
            'battery electric
            VKmVType(6) += (RVFCatTraf(c, 1, 6) + RVFCatTraf(c, 2, 6) + RVFCatTraf(c, 5, 6))
            'LPG
            VKmVType(7) += (RVFCatTraf(c, 2, 7) + RVFCatTraf(c, 5, 7))
            'CNG
            VKmVType(8) += (RVFCatTraf(c, 2, 8) + RVFCatTraf(c, 5, 8))
            'hydrogen ICE 
            VKmVType(9) += (RVFCatTraf(c, 1, 9) + RVFCatTraf(c, 3, 9) + RVFCatTraf(c, 4, 9))
            'hydrogen fuel cell
            VKmVType(10) += (RVFCatTraf(c, 1, 10) + RVFCatTraf(c, 3, 10) + RVFCatTraf(c, 4, 10) + RVFCatTraf(c, 5, 10))
        Next

        'write disaggregated fuel outputs to file
        FuelArray(ZoneID, 0) = ZoneID
        FuelArray(ZoneID, 1) = YearCount
        'car petrol
        CatFuelTotal = RVFFuel(1, 1, 1) + RVFFuel(2, 1, 1) + RVFFuel(3, 1, 1) + RVFFuel(4, 1, 1) + RVFFuel(1, 1, 3) + RVFFuel(2, 1, 3) + RVFFuel(3, 1, 3) + RVFFuel(4, 1, 3) + RVFFuel(1, 1, 5) + RVFFuel(2, 1, 5) + RVFFuel(3, 1, 5)
        FuelArray(ZoneID, 2) = CatFuelTotal
        'LGV petrol
        CatFuelTotal = RVFFuel(1, 2, 1) + RVFFuel(2, 2, 1) + RVFFuel(3, 2, 1) + RVFFuel(4, 2, 1)
        FuelArray(ZoneID, 3) = CatFuelTotal
        'car diesel
        CatFuelTotal = RVFFuel(1, 1, 2) + RVFFuel(2, 1, 2) + RVFFuel(3, 1, 2) + RVFFuel(4, 1, 2) + RVFFuel(1, 1, 4) + RVFFuel(2, 1, 4) + RVFFuel(3, 1, 4) + RVFFuel(4, 1, 4)
        FuelArray(ZoneID, 4) = CatFuelTotal
        'LGV diesel
        CatFuelTotal = RVFFuel(1, 2, 2) + RVFFuel(2, 2, 2) + RVFFuel(3, 2, 2) + RVFFuel(4, 2, 2) + RVFFuel(1, 2, 4) + RVFFuel(2, 2, 4) + RVFFuel(3, 2, 4) + RVFFuel(4, 2, 4) + RVFFuel(1, 2, 5) + RVFFuel(2, 2, 5) + RVFFuel(3, 2, 5)
        FuelArray(ZoneID, 5) = CatFuelTotal
        'HGV2-3axle diesel
        CatFuelTotal = RVFFuel(1, 3, 2) + RVFFuel(2, 3, 2) + RVFFuel(3, 3, 2) + RVFFuel(4, 3, 2) + RVFFuel(1, 3, 4) + RVFFuel(2, 3, 4) + RVFFuel(3, 3, 4) + RVFFuel(4, 3, 4)
        FuelArray(ZoneID, 6) = CatFuelTotal
        'HGV 4 axle diesel
        CatFuelTotal = RVFFuel(1, 4, 2) + RVFFuel(2, 4, 2) + RVFFuel(3, 4, 2) + RVFFuel(4, 4, 2) + RVFFuel(1, 4, 4) + RVFFuel(2, 4, 4) + RVFFuel(3, 4, 4) + RVFFuel(4, 4, 4)
        FuelArray(ZoneID, 7) = CatFuelTotal
        'PSV diesel
        CatFuelTotal = RVFFuel(1, 5, 2) + RVFFuel(2, 5, 2) + RVFFuel(3, 5, 2) + RVFFuel(4, 5, 2) + RVFFuel(1, 5, 4) + RVFFuel(2, 5, 4) + RVFFuel(3, 5, 4) + RVFFuel(4, 5, 4) + RVFFuel(1, 5, 5) + RVFFuel(2, 5, 5) + RVFFuel(3, 5, 5)
        FuelArray(ZoneID, 8) = CatFuelTotal
        'car electric
        CatFuelTotal = RVFFuel(4, 1, 5) + RVFFuel(1, 1, 6) + RVFFuel(2, 1, 6) + RVFFuel(3, 1, 6) + RVFFuel(4, 1, 6)
        FuelArray(ZoneID, 9) = CatFuelTotal
        'LGV electric
        CatFuelTotal = RVFFuel(4, 2, 5) + RVFFuel(1, 2, 6) + RVFFuel(2, 2, 6) + RVFFuel(3, 2, 6) + RVFFuel(4, 2, 6)
        FuelArray(ZoneID, 10) = CatFuelTotal
        'PSV electric
        CatFuelTotal = RVFFuel(4, 5, 5) + RVFFuel(1, 5, 6) + RVFFuel(2, 5, 6) + RVFFuel(3, 5, 6) + RVFFuel(4, 5, 6)
        FuelArray(ZoneID, 11) = CatFuelTotal
        'LGV LPG
        CatFuelTotal = RVFFuel(1, 2, 7) + RVFFuel(2, 2, 7) + RVFFuel(3, 2, 7) + RVFFuel(4, 2, 7)
        FuelArray(ZoneID, 12) = CatFuelTotal
        'PSV LPG
        CatFuelTotal = RVFFuel(1, 5, 7) + RVFFuel(2, 5, 7) + RVFFuel(3, 5, 7) + RVFFuel(4, 5, 7)
        FuelArray(ZoneID, 13) = CatFuelTotal
        'LGV CNG
        CatFuelTotal = RVFFuel(1, 2, 8) + RVFFuel(2, 2, 8) + RVFFuel(3, 2, 8) + RVFFuel(4, 2, 8)
        FuelArray(ZoneID, 14) = CatFuelTotal
        'PSV CNG
        CatFuelTotal = RVFFuel(1, 5, 8) + RVFFuel(2, 5, 8) + RVFFuel(3, 5, 8) + RVFFuel(4, 5, 8)
        FuelArray(ZoneID, 15) = CatFuelTotal
        'car hydrogen
        CatFuelTotal = RVFFuel(1, 1, 9) + RVFFuel(2, 1, 9) + RVFFuel(3, 1, 9) + RVFFuel(4, 1, 9) + RVFFuel(1, 1, 10) + RVFFuel(2, 1, 10) + RVFFuel(3, 1, 10) + RVFFuel(4, 1, 10)
        FuelArray(ZoneID, 16) = CatFuelTotal
        'HGV 2-3 axle hydrogen
        CatFuelTotal = RVFFuel(1, 3, 9) + RVFFuel(2, 3, 9) + RVFFuel(3, 3, 9) + RVFFuel(4, 3, 9) + RVFFuel(1, 3, 10) + RVFFuel(2, 3, 10) + RVFFuel(3, 3, 10) + RVFFuel(4, 3, 10)
        FuelArray(ZoneID, 17) = CatFuelTotal
        'HGV 4+ axle hydrogen
        CatFuelTotal = RVFFuel(1, 4, 9) + RVFFuel(2, 4, 9) + RVFFuel(3, 4, 9) + RVFFuel(4, 4, 9) + RVFFuel(1, 4, 10) + RVFFuel(2, 4, 10) + RVFFuel(3, 4, 10) + RVFFuel(4, 4, 10)
        FuelArray(ZoneID, 18) = CatFuelTotal
        'PSV hydrogen
        CatFuelTotal = RVFFuel(1, 5, 10) + RVFFuel(2, 5, 10) + RVFFuel(3, 5, 10) + RVFFuel(4, 5, 10)
        FuelArray(ZoneID, 19) = CatFuelTotal

    End Sub

    Sub VehicleFuelConsumption()
        Dim alpha As Double
        Dim beta As Double
        Dim gamma As Double
        Dim zeta As Double
        'set the parameter values (taken from table in the model documentation, from Transport Scotland data)
        Select Case VClass
            Case "CarP"
                alpha = 0.964022581
                beta = 0.041448033
                gamma = -0.0000454163
                zeta = 0.00000201346
            Case "CarD"
                alpha = 0.437094041
                beta = 0.058616489
                gamma = -0.00052488
                zeta = 0.00000412709
            Case "LGVP"
                alpha = 1.556463336
                beta = 0.064253318
                gamma = -0.000744481
                zeta = 0.0000100552
            Case "LGVD"
                alpha = 1.045268333
                beta = 0.057901415
                gamma = -0.000432895
                zeta = 0.0000080252
            Case "HGV1D"
                alpha = 1.477368474
                beta = 0.245615208
                gamma = -0.003572413
                zeta = 0.000030638
            Case "HGV2D"
                alpha = 3.390702946
                beta = 0.394379054
                gamma = -0.004642285
                zeta = 0.0000359224
            Case "PSVD"
                alpha = 4.115603124
                beta = 0.306464813
                gamma = -0.00420643
                zeta = 0.0000365263
        End Select
        'v1.4 modification to stop generating errors where there is no traffic on a given road type
        If FuelSpeed > 0 Then
            FuelPerKm = (alpha + (beta * FuelSpeed) + (gamma * (FuelSpeed ^ 2)) + (zeta * (FuelSpeed ^ 3))) / FuelSpeed
        Else
            FuelPerKm = 0
        End If

    End Sub

    Sub WriteRoadZoneOutput()

        'write to output array
        OutputArray(ZoneID, 0) = YearCount
        OutputArray(ZoneID, 1) = ZoneID
        OutputArray(ZoneID, 2) = NewVkm
        OutputArray(ZoneID, 3) = ZoneSpdNew
        OutputArray(ZoneID, 4) = PetrolUsed
        OutputArray(ZoneID, 5) = DieselUsed
        OutputArray(ZoneID, 6) = ElectricUsed
        OutputArray(ZoneID, 7) = LPGUsed
        OutputArray(ZoneID, 8) = CNGUsed
        OutputArray(ZoneID, 9) = HydrogenUsed
        OutputArray(ZoneID, 10) = RoadCatTraffic(ZoneID, 1)
        OutputArray(ZoneID, 11) = RoadCatTraffic(ZoneID, 2)
        OutputArray(ZoneID, 12) = RoadCatTraffic(ZoneID, 3)
        OutputArray(ZoneID, 13) = RoadCatTraffic(ZoneID, 4)
        OutputArray(ZoneID, 14) = NewCatSpeed(ZoneID, 1)
        OutputArray(ZoneID, 15) = NewCatSpeed(ZoneID, 2)
        OutputArray(ZoneID, 16) = NewCatSpeed(ZoneID, 3)
        OutputArray(ZoneID, 17) = NewCatSpeed(ZoneID, 4)
        For v = 1 To 10
            OutputArray(ZoneID, 17 + v) = VKmVType(v)
        Next

        If BuildInfra = True Then
            'first clear 'BuiltLaneKm' array
            For k = 1 To 4
                BuiltLaneKm(ZoneID, k) = 0
            Next
            'check motorways
            If RoadCatTraffic(ZoneID, 1) > (0.9 * BaseCatC(ZoneID, 1)) Then
                BuiltLaneKm(ZoneID, 1) = (NewLaneKm(1) * 0.1)
                NewCapArray(NewCapNum, 0) = ZoneID
                NewCapArray(NewCapNum, 1) = YearCount
                NewCapArray(NewCapNum, 2) = BuiltLaneKm(ZoneID, 1)
                NewCapNum += 1
            End If
            'check rural a roads
            If RoadCatTraffic(ZoneID, 2) > (0.9 * BaseCatC(ZoneID, 2)) Then
                BuiltLaneKm(ZoneID, 2) = (NewLaneKm(2) * 0.1)
                NewCapArray(NewCapNum, 0) = ZoneID
                NewCapArray(NewCapNum, 1) = YearCount
                NewCapArray(NewCapNum, 3) = BuiltLaneKm(ZoneID, 2)
                NewCapNum += 1
            End If
            'check rural minor roads
            If RoadCatTraffic(ZoneID, 3) > (0.9 * BaseCatC(ZoneID, 3)) Then
                BuiltLaneKm(ZoneID, 3) = (NewLaneKm(3) * 0.1)
                NewCapArray(NewCapNum, 0) = ZoneID
                NewCapArray(NewCapNum, 1) = YearCount
                NewCapArray(NewCapNum, 4) = BuiltLaneKm(ZoneID, 3)
                NewCapNum += 1
            End If
            'check urban roads
            If RoadCatTraffic(ZoneID, 4) > (0.9 * BaseCatC(ZoneID, 4)) Then
                BuiltLaneKm(ZoneID, 4) = (NewLaneKm(4) * 0.1)
                NewCapArray(NewCapNum, 0) = ZoneID
                NewCapArray(NewCapNum, 1) = YearCount
                NewCapArray(NewCapNum, 5) = BuiltLaneKm(ZoneID, 4)
                NewCapNum += 1
            End If
        End If

        'update variables
        ZonePop(ZoneID, 1) = ZoneExtVar(ZoneID, 2)
        ZoneGVA(ZoneID, 1) = ZoneExtVar(ZoneID, 3)
        ZoneSpeed(ZoneID, 1) = ZoneSpdNew
        ZoneCarCost(ZoneID, 1) = ZoneExtVar(ZoneID, 4)
        ZoneLGVCost(ZoneID, 1) = ZoneExtVar(ZoneID, 42)
        ZoneHGV1Cost(ZoneID, 1) = ZoneExtVar(ZoneID, 43)
        ZoneHGV2Cost(ZoneID, 1) = ZoneExtVar(ZoneID, 44)
        ZonePSVCost(ZoneID, 1) = ZoneExtVar(ZoneID, 45)
        BaseVkm(ZoneID, 1) = NewVkm
        ZoneLaneKm(ZoneID, 1) = ZoneExtVar(ZoneID, 6)
        ZoneLaneKm(ZoneID, 2) = CDbl(ZoneExtVar(ZoneID, 7)) + CDbl(ZoneExtVar(ZoneID, 8))
        ZoneLaneKm(ZoneID, 3) = ZoneExtVar(ZoneID, 9)
        ZoneLaneKm(ZoneID, 4) = CDbl(ZoneExtVar(ZoneID, 10)) + CDbl(ZoneExtVar(ZoneID, 11))
        RoadCatKm(ZoneID, 1) = ZoneExtVar(ZoneID, 6) / 6
        RoadCatKm(ZoneID, 2) = (CDbl(ZoneExtVar(ZoneID, 7)) / 4) + (CDbl(ZoneExtVar(ZoneID, 8)) / 2)
        RoadCatKm(ZoneID, 3) = ZoneExtVar(ZoneID, 9) / 2
        RoadCatKm(ZoneID, 4) = (CDbl(ZoneExtVar(ZoneID, 10)) / 4) + (CDbl(ZoneExtVar(ZoneID, 11)) / 2)
        PetrolUsed = 0
        DieselUsed = 0
        ElectricUsed = 0
        LPGUsed = 0
        CNGUsed = 0
        HydrogenUsed = 0

        'write to temp array
        TempArray(ZoneID, 0) = YearCount
        TempArray(ZoneID, 1) = ZoneID
        TempArray(ZoneID, 2) = ZonePop(ZoneID, 1)
        TempArray(ZoneID, 3) = ZoneGVA(ZoneID, 1)
        TempArray(ZoneID, 4) = ZoneSpeed(ZoneID, 1)
        TempArray(ZoneID, 5) = ZoneCarCost(ZoneID, 1)
        TempArray(ZoneID, 6) = ZoneLGVCost(ZoneID, 1)
        TempArray(ZoneID, 7) = ZoneHGV1Cost(ZoneID, 1)
        TempArray(ZoneID, 8) = ZoneHGV2Cost(ZoneID, 1)
        TempArray(ZoneID, 9) = ZonePSVCost(ZoneID, 1)
        TempArray(ZoneID, 10) = BaseVkm(ZoneID, 1)
        TempArray(ZoneID, 11) = ZoneLaneKm(ZoneID, 1)
        TempArray(ZoneID, 12) = ZoneLaneKm(ZoneID, 2)
        TempArray(ZoneID, 13) = ZoneLaneKm(ZoneID, 3)
        TempArray(ZoneID, 14) = ZoneLaneKm(ZoneID, 4)
        TempArray(ZoneID, 15) = RoadCatKm(ZoneID, 1)
        TempArray(ZoneID, 16) = RoadCatKm(ZoneID, 2)
        TempArray(ZoneID, 17) = RoadCatKm(ZoneID, 3)
        TempArray(ZoneID, 18) = RoadCatKm(ZoneID, 4)
        For x = 0 To 3
            TempArray(ZoneID, 19 + 6 * (x)) = RoadCatTraffic(ZoneID, x + 1)
            For y = 0 To 4
                TempArray(ZoneID, 20 + 6 * (x) + y) = RVCatTraf(x + 1, y + 1)
            Next
        Next
        For x = 0 To 3
            For y = 0 To 3
                TempArray(ZoneID, 43 + 4 * (x) + y) = SuppressedTraffic(x + 1, y + 1)
            Next
        Next
        For x = 0 To 3
            TempArray(ZoneID, 59 + x) = NewCatSpeed(ZoneID, x + 1)
        Next
        For x = 1 To 4
            TempArray(ZoneID, 62 + x) = Latentvkm(ZoneID, x)
        Next

        For x = 1 To 4
            TempArray(ZoneID, 66 + x) = AddedLaneKm(ZoneID, x)
        Next

        For x = 1 To 4
            TempArray(ZoneID, 70 + x) = BuiltLaneKm(ZoneID, x)
        Next

        'clear previous suppressed traffic
        If SmarterChoices = True Then
            SuppressedTraffic(4, 1) = 0
        End If
        If UrbanFrt = True Then
            For y = 2 To 4
                SuppressedTraffic(4, y) = 0
            Next
        End If
        If SmartFrt = True Then
            For x = 1 To 3
                For y = 3 To 4
                    SuppressedTraffic(x, y) = 0
                Next
            Next
        End If



    End Sub



End Module
