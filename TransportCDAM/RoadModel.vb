Module RoadModel
    'This version is complete, and dependent on module FullCDAM for file paths.  It also now allows elasticities to vary over time.
    'version 1.2 has a revised corrected speed-flow relationship, and incorporates latent demand variables etc
    'now also includes differential costs for different vehicle types
    'now also has the option to build additional infrastructure if capacity exceeds a certain level
    'now includes a congestion charging option
    'v1.3 corrects the procedure for dealing with latent demand when capacity is added
    'v1.3 also includes smart logistics option for hgv traffic
    'now also includes variable trip rate option
    'v1.4 replaces CostNew(sc) with CostNew(sc,h)
    'v1.6 recode to calculate by annual timesteps, parameters' dimensions are increased by one to store for each roadlink to avoid override
    'run time has been increased because the increased dimension requires extra memory and the temp file needs to be read and write for every step, but still acceptable
    'v1.7 now corporate with Database function, read/write are using the function in database interface
    'now all file related functions are using databaseinterface
    '1.9 now the module can run with database connection and read/write from/to database

    Dim InputFlow As String
    Dim FlowDetails() As String
    Dim FlowID(291, 1) As Long
    Dim Zone1(291, 1) As String
    Dim Zone2(291, 1) As String
    'SpeedCatFlows is an array containing the flows in all the speed categories
    Dim SpeedCatFlows(291, 20) As Double
    Dim Z1Pop(291, 1) As Double
    Dim Z2Pop(291, 1) As Double
    Dim Z1GVA(291, 1) As Double
    Dim Z2GVA(291, 1) As Double
    Dim CostOld(291, 19, 24), CostNew(19, 24) As Double
    Dim MStartFlow() As Long
    Dim DStartFlow() As Long
    Dim SStartFlow() As Long
    Dim HourProportions(24) As Double
    Dim TimeProfile(1, 23) As String
    Dim OldHourlyFlows(291, 20, 24) As Double
    Dim HourlySpeeds(291, 20, 24) As Double
    Dim RoadTypeFlows(291, 2, 23) As Double
    Dim RoadTypeFlowsNew As Double
    Dim sc As Integer
    Dim h As Integer
    Dim FreeFlowSpeeds(20, 2) As String
    Dim MaxCap(291, 2) As String
    Dim SpeedNew As Double
    Dim SpeedOld As Double
    Dim SpeedOriginal As Double
    Dim FlowOld As Double
    Dim FlowNew As Double
    Dim SpeedRatio As Double
    Dim PFlowRatio(19) As Double
    Dim FFlowRatio(19) As Double
    Dim FlowRatio As Double
    Dim ExternalValues(291, 33) As String
    Dim ScaledHourlyFlows(20, 24) As Double
    Dim NewHourlyFlows(20, 24) As Double
    Dim NewHourlySpeeds(291, 20, 24) As Double
    Dim RoadType As Byte
    Dim SpeedCatFlowsNew(20) As Double
    Dim SpeedCatSpeedsNew(20) As Double
    Dim TotalFlowNew As Double
    Dim MeanSpeedNew As Double
    Dim OutputRow As String
    'These local variables store each of the model variable ratios
    Dim rat1, rat3, rat5 As Double
    Dim ratf1, ratf3, ratf5 As Double
    Dim rat6(19), ratf6(19) As Double
    Dim ClassFlow As Double
    Dim ClassFlowNew As Double
    Dim MwayFlowNew As Double
    Dim MWaySpdNew As Double
    Dim DualFlowNew As Double
    Dim DualSpdNew As Double
    Dim SingFlowNew As Double
    Dim SingSpdNew As Double
    Dim TotalLanesOriginal(291, 1) As Long
    Dim RoadTypeLanes(291, 2) As Integer
    Dim RoadTypeLanesNew(291, 2) As Integer
    Dim TotalLanesOld As Long
    Dim TotalLanesNew As Long
    Dim CapChangeNew(291, 1) As Boolean
    Dim RoadEls(90, 11) As String
    Dim FreeFlowCU As Double
    'v1.2 this is a new variable, storing latent demand
    Dim LatentHourlyFlows(291, 20, 24) As Double
    'v1.2 this is also new, storing total latent demand for each speed category
    Dim LatentFlows(20) As Double
    'this is just a temporary holding variable
    Dim LatentTraffic As Double
    Dim MWayLatFlowNew, DualLatFlowNew, SingLatFlowNew As Double
    Dim MFullHrs, DFullHrs, SFullHrs As Long
    Dim AddedLanes(291, 2) As Integer
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim StandingCosts(291, 19) As Double
    Dim ChargeNew(19, 24), ChargeOld(291, 19, 24) As Double
    Dim RdTripRates(1, 90) As Double
    Dim MeanCostNew(2) As Double
    Dim row As String
    Dim elstring() As String
    Dim elcount As Integer
    Dim link As Long
    Dim output As String
    Dim MWH As Long
    Dim DCH As Long
    Dim SCH As Long
    Dim InputArray(291, 54) As String
    Dim OutputArray(292, 59) As String
    Dim TempArray(292, 3037) As String
    Dim TempAnArray(292, 34) As String
    Dim TempHArray(7007, 87) As String
    Dim NewCapArray(291, 3) As String
    Dim NewCapNum As Integer


    Public Sub RoadLinkMain()

        'for year 2010
        Dim yearIs2010 As Boolean = False
        If g_modelRunYear = 2010 Then
            'create data for year 2010
            g_modelRunYear += 1
            'Call Year2010()
            yearIs2010 = True
            'Exit Sub
        End If

        'read all related files
        Call SetFiles()

        'load the daily travel proportions
        Call DailyProfile()

        'v1.2 modification: for the moment we are saying that if capacity utilisation is less than 0.25 then traffic flows at free flow speed - but may wish to alter this
        FreeFlowCU = 0.25


        'load external variables
        If yearIs2010 = True Then
            Call ReadData("RoadLink", "ExtVar", ExternalValues, g_modelRunYear - 1)

        Else
            Call ReadData("RoadLink", "ExtVar", ExternalValues, g_modelRunYear)

        End If

        'read from initial file if year 1, otherwise update from temp file
        If g_modelRunYear = g_initialYear Then
            Call ReadData("RoadLink", "Input", InputArray, g_modelRunYear)
        Else
            Call ReadData("RoadLink", "Temp Annual", TempAnArray, g_modelRunYear)
            Call ReadData("RoadLink", "Temp Hourly", TempHArray, g_modelRunYear)
        End If

        link = 1
        'loop through all links
        Do Until link > 291


            'modification v1.2 - input file replaced by internally specified values - because these have to be altered for some links
            'further modification - these are now specified in the input file (and therefore any alterations where base usage exceeds the theoretical maximum have to be made in the input file)

            'set the new cap array to the first line to start with
            NewCapNum = 1

            'read from input array
            Call LoadInputRow()

            'calculate the starting hourly flows
            If g_modelRunYear = g_initialYear Then
                Call StartFlows()
            End If

            'check if new capacity has been added
            Call CapChange()

            'alter speed category flows if capacity has changed
            'modification completed v1.2 - now also adds latent demand to the previous demand figures, and blanks the relevant latent demand values
            Call NewSpeedCatFlows()

            'calculate the base speeds
            Call BaseSpeeds()

            'calculate each of the hourly flows
            Call HourlyFlowCalc()

            'sum all the hourly flows to give an equivalent AADF figure
            Call TotalFlow()

            If yearIs2010 = True Then g_modelRunYear -= 1
            'write the flows to the output file and temp file
            Call WriteOutputRow()
            If yearIs2010 = True Then g_modelRunYear += 1

            link += 1
        Loop

        'create file is true if it is the initial year, otherwise update existing files
        'in database version, write to Temp Annual and Temp Hourly tables
        Call WriteData("RoadLink", "Output", OutputArray, , False)
        Call WriteData("RoadLink", "Temp Annual", TempAnArray, , False)
        Call WriteData("RoadLink", "Temp Hourly", TempHArray, , False)

        If BuildInfra = True Then
            Call WriteData("RoadLink", "NewCap_Added", NewCapArray, , False)
        End If
        Erase TempAnArray
        Erase TempHArray

        If yearIs2010 = True Then g_modelRunYear -= 1

    End Sub

    Sub SetFiles()

        'load the time profiles from database
        Call ReadData("RoadLink", "FreeFlowSpeeds", FreeFlowSpeeds, g_modelRunYear)

        If UpdateExtVars = True Then
            If NewRdLCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If


        'load the elasticities
        Call ReadData("RoadLink", "Elasticity", RoadEls, g_modelRunYear)

        'if using variable trip rates then set up the trip rate variable
        If TripRates = True Then
            'get the strat values
            RdTripRates(0, 1) = stratarray(1, 93)
            RdTripRates(1, 1) = stratarray(1, 94)
        End If

    End Sub

    Sub DailyProfile()
        'sets up an array giving the proportion of trips made in each hour of the day

        'load the time profiles from database
        Call ReadData("RoadLink", "DailyProfile", TimeProfile, g_modelRunYear)

        Dim d As Byte

        d = 1

        Do While d < 25
            HourProportions(d - 1) = TimeProfile(d, 2)
            d += 1
        Loop

    End Sub

    Sub LoadInputRow()
        Dim varnum As Long
        Dim i As Long
        Dim hrow As Integer

        If g_modelRunYear = g_initialYear Then
            'read base year (year 0) data
            FlowID(link, 1) = InputArray(link, 1)
            Zone1(link, 1) = InputArray(link, 2)
            Zone2(link, 1) = InputArray(link, 3)
            RoadTypeLanes(link, 0) = InputArray(link, 4)
            RoadTypeLanes(link, 1) = InputArray(link, 5)
            RoadTypeLanes(link, 2) = InputArray(link, 6)
            'need the total lanes to start with as stable variable - this is because once there has been any capacity change will need to recalculate the base flows per lane each year
            TotalLanesOriginal(link, 1) = RoadTypeLanes(link, 0) + RoadTypeLanes(link, 1) + RoadTypeLanes(link, 2)
            'set road type lanes new equal to road type lanes to start with, otherwise it will assume a capacity change to start with
            RoadTypeLanesNew(link, 0) = InputArray(link, 4)
            RoadTypeLanesNew(link, 1) = InputArray(link, 5)
            RoadTypeLanesNew(link, 2) = InputArray(link, 6)
            SpeedCatFlows(link, 0) = InputArray(link, 8)
            SpeedCatFlows(link, 1) = InputArray(link, 9)
            SpeedCatFlows(link, 2) = InputArray(link, 10)
            SpeedCatFlows(link, 3) = InputArray(link, 11)
            SpeedCatFlows(link, 4) = InputArray(link, 12)
            SpeedCatFlows(link, 5) = InputArray(link, 13)
            SpeedCatFlows(link, 6) = InputArray(link, 14)
            SpeedCatFlows(link, 7) = InputArray(link, 15)
            SpeedCatFlows(link, 8) = InputArray(link, 16)
            SpeedCatFlows(link, 9) = InputArray(link, 17)
            SpeedCatFlows(link, 10) = InputArray(link, 18)
            SpeedCatFlows(link, 11) = InputArray(link, 19)
            SpeedCatFlows(link, 12) = InputArray(link, 20)
            SpeedCatFlows(link, 13) = InputArray(link, 21)
            SpeedCatFlows(link, 14) = InputArray(link, 22)
            SpeedCatFlows(link, 15) = InputArray(link, 23)
            SpeedCatFlows(link, 16) = InputArray(link, 24)
            SpeedCatFlows(link, 17) = InputArray(link, 25)
            SpeedCatFlows(link, 18) = InputArray(link, 26)
            SpeedCatFlows(link, 19) = InputArray(link, 27)
            Z1Pop(link, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, Zone1(link, 1), "OZ", "'road'")
            Z2Pop(link, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, Zone2(link, 1), "DZ", "'road'")
            Z1GVA(link, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, Zone1(link, 1), "OZ", "'road'")
            Z2GVA(link, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, Zone2(link, 1), "DZ", "'road'")
            '24 hours for category 0
            For c = 0 To 23
                CostOld(link, 0, c) = InputArray(link, 28)
            Next
            MaxCap(link, 0) = InputArray(link, 29)
            MaxCap(link, 1) = InputArray(link, 30)
            MaxCap(link, 2) = InputArray(link, 31)
            varnum = 32
            '24 hours for category 1 - 19
            For x = 1 To 19
                For c = 0 To 23
                    CostOld(link, x, c) = InputArray(link, varnum)
                Next
                varnum += 1
            Next
            'if using congestion charge then get fixed costs
            If CongestionCharge = True Then
                StandingCosts(link, 0) = CostOld(link, 0, 1) * 0.6936
                StandingCosts(link, 1) = CostOld(link, 1, 1) * ((0.7663 * 0.598) + (0.8089 * 0.402))
                StandingCosts(link, 2) = CostOld(link, 2, 1) * 0.845
                StandingCosts(link, 3) = CostOld(link, 3, 1) * 0.8699
                StandingCosts(link, 4) = CostOld(link, 4, 1) * 0.7791
                StandingCosts(link, 5) = CostOld(link, 5, 1) * 0.7065
                StandingCosts(link, 6) = CostOld(link, 6, 1) * 0.6936
                StandingCosts(link, 7) = CostOld(link, 7, 1) * ((0.7663 * 0.598) + (0.8089 * 0.402))
                StandingCosts(link, 8) = CostOld(link, 8, 1) * 0.845
                StandingCosts(link, 9) = CostOld(link, 9, 1) * 0.8699
                StandingCosts(link, 10) = CostOld(link, 10, 1) * 0.7791
                StandingCosts(link, 11) = CostOld(link, 11, 1) * 0.7065
                StandingCosts(link, 12) = CostOld(link, 12, 1) * 0.6936
                StandingCosts(link, 13) = CostOld(link, 13, 1) * ((0.7663 * 0.598) + (0.8089 * 0.402))
                StandingCosts(link, 14) = CostOld(link, 14, 1) * 0.845
                StandingCosts(link, 15) = CostOld(link, 15, 1) * 0.8699
                StandingCosts(link, 16) = CostOld(link, 16, 1) * 0.7791
                StandingCosts(link, 17) = CostOld(link, 17, 1) * 0.7791
                StandingCosts(link, 18) = CostOld(link, 18, 1) * 0.7065
                StandingCosts(link, 19) = CostOld(link, 19, 1) * 0.7065
            End If
            AddedLanes(link, 0) = 0
            AddedLanes(link, 1) = 0
            AddedLanes(link, 2) = 0

            'set capacity changed checker to false
            CapChangeNew(link, 1) = False

            'clear previous values
            ReDim ChargeNew(19, 24)

        Else
            'if not first year, read the values 
            ' from the Temp Annual array
            FlowID(link, 1) = TempAnArray(link, 3)
            RoadTypeLanes(link, 0) = TempAnArray(link, 4)
            RoadTypeLanes(link, 1) = TempAnArray(link, 5)
            RoadTypeLanes(link, 2) = TempAnArray(link, 6)
            RoadTypeLanesNew(link, 0) = TempAnArray(link, 7)
            RoadTypeLanesNew(link, 1) = TempAnArray(link, 8)
            RoadTypeLanesNew(link, 2) = TempAnArray(link, 9)
            For x = 0 To 19
                SpeedCatFlows(link, x) = TempAnArray(link, 10 + x)
            Next
            MaxCap(link, 0) = TempAnArray(link, 30)
            MaxCap(link, 1) = TempAnArray(link, 31)
            MaxCap(link, 2) = TempAnArray(link, 32)
            AddedLanes(link, 0) = TempAnArray(link, 33)
            AddedLanes(link, 1) = TempAnArray(link, 34)
            AddedLanes(link, 2) = TempAnArray(link, 35)

            'Read from the Temp Hourly Array
            'Loop through the hourly data for each roadtype (RoadType) and speed capacity (sc)
            h = 0
            Do Until h > 23
                'work out the row number for the hourly data
                hrow = (link - 1) * 24 + h + 1
                RoadTypeFlows(link, 0, h) = TempHArray(hrow, 5)
                RoadTypeFlows(link, 1, h) = TempHArray(hrow, 6)
                RoadTypeFlows(link, 2, h) = TempHArray(hrow, 7)
                i = 8
                sc = 0
                Do While sc < 20
                    RoadType = AssignRoadType(sc)

                    OldHourlyFlows(link, sc, h) = TempHArray(hrow, i)
                    i += 1
                    ChargeOld(link, sc, h) = TempHArray(hrow, i)
                    i += 1
                    LatentHourlyFlows(link, sc, h) = TempHArray(hrow, i)
                    i += 1
                    NewHourlySpeeds(link, sc, h) = TempHArray(hrow, i)
                    i += 1

                    sc += 1
                Loop
                h += 1
            Loop

            'get zone1 ID and zone2 ID
            get_zone_by_flowid(FlowID(link, 1), Zone1(link, 1), Zone2(link, 1), "road")
            'Get Population and GVA data
            Z1Pop(link, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, Zone1(link, 1), "OZ", "'road'")
            Z2Pop(link, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, Zone2(link, 1), "DZ", "'road'")
            Z1GVA(link, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, Zone1(link, 1), "OZ", "'road'")
            Z2GVA(link, 1) = get_gva_data_by_zoneID(g_modelRunYear - 1, Zone2(link, 1), "DZ", "'road'")



        End If
    End Sub

    Sub NewSpeedCatFlows()
        'if capacity has changed in the last year
        If CapChangeNew(link, 1) = True Then

            'start with the first road link category
            sc = 0

            Do While sc < 20
                RoadType = AssignRoadType(sc)
                If RoadTypeLanesNew(link, RoadType) > 0 Then
                    If SpeedCatFlows(link, sc) > 0 Then
                        SpeedCatFlows(link, sc) = (SpeedCatFlows(link, sc) * RoadTypeLanes(link, RoadType)) / RoadTypeLanesNew(link, RoadType)
                    End If
                    'v1.4 modification - need to scale these figures too
                    For hr = 0 To 23
                        OldHourlyFlows(link, sc, hr) = (OldHourlyFlows(link, sc, hr) * RoadTypeLanes(link, RoadType)) / RoadTypeLanesNew(link, RoadType)
                        RoadTypeFlows(link, RoadType, hr) = (RoadTypeFlows(link, RoadType, hr) * RoadTypeLanes(link, RoadType)) / RoadTypeLanesNew(link, RoadType)
                    Next hr
                    '***v1.4 modification finishes here
                End If
                'v1.2 modification - add latent demand to the previous demand, and set latent demand variables to zero
                'v1.2 additional modification - also update the road type flows variables
                'calculate for all 24 hours
                h = 0
                Do While h < 24
                    OldHourlyFlows(link, sc, h) += LatentHourlyFlows(link, sc, h)
                    RoadTypeFlows(link, RoadType, h) += LatentHourlyFlows(link, sc, h)
                    LatentHourlyFlows(link, sc, h) = 0
                    h += 1
                Loop
                sc += 1
            Loop

        Else
            'if not then don't need to do anything
        End If

        'update road type lane using previous year's calculation result
        RoadTypeLanes(link, 0) = RoadTypeLanesNew(link, 0)
        RoadTypeLanes(link, 1) = RoadTypeLanesNew(link, 1)
        RoadTypeLanes(link, 2) = RoadTypeLanesNew(link, 2)


    End Sub

    Sub StartFlows()
        Dim t As Integer

        'divide the total flows for each flow type by the proportions given by the daily travel profile
        'store these proportional flows in a two dimensional array
        'sum the hourly values to give a total value for each road type
        'note that in this and subsequent subs the 'sc' and 'h' values are 1 lower than might 'intuitively' be expected, because first element in arrays is numbered 0


        sc = 0
        h = 0

        'for all 20 road link categories and 24 hours
        Do While sc < 20
            RoadType = AssignRoadType(sc)
            Do While h < 24
                OldHourlyFlows(link, sc, h) = SpeedCatFlows(link, sc) * HourProportions(h)
                RoadTypeFlows(link, RoadType, h) = RoadTypeFlows(link, RoadType, h) + OldHourlyFlows(link, sc, h)
                h += 1
            Loop
            sc += 1
            h = 0
        Loop

        'v1.2 completed modification check if the hourly flow in the busiest hour (0800-0900, so h=8) exceeds the maximum capacity for the road type, and if it does then update that maximum capacity
        'the maximum capacity is rounded up to the nearest whole number to overcome problems caused by variables storing a different number of decimal places
        For t = 0 To 2
            If RoadTypeFlows(link, t, 8) > MaxCap(link, t) Then
                MaxCap(link, t) = Math.Round(RoadTypeFlows(link, t, 8) + 0.5)
            End If
        Next
    End Sub

    Sub BaseSpeeds()
        'calculate the speed for each of the hourly segments for each of the speed categories
        'in this case don't need to iterate to get speeds - we know the total base flow for each road type, so just use the speed calculator to adjust the speeds if conditions are congested - flows are observed and therefore held constant
        'if this is the first year, we need to calculate the base speeds from the input data

        If g_modelRunYear = g_initialYear Then
            sc = 0
            h = 0

            'v1.2 mod update the maximum capacity values
            'v1.3 mod moved inside the if clause as otherwise fails in 1st year if maximum capacity has been reset in base year
            If ExternalValues(link, 11) > MaxCap(link, 0) Then
                MaxCap(link, 0) = ExternalValues(link, 11)
            End If
            If ExternalValues(link, 12) > MaxCap(link, 1) Then
                MaxCap(link, 1) = ExternalValues(link, 12)
            End If
            If ExternalValues(link, 13) > MaxCap(link, 2) Then
                MaxCap(link, 2) = ExternalValues(link, 13)
            End If


            'for all 20 road link categories and 24 hours

            Do While sc < 20
                Do While h < 24
                    RoadType = AssignRoadType(sc)
                    'if traffic less than free flow capacity then adopt free flow speed
                    If RoadTypeFlows(link, RoadType, h) < (FreeFlowCU * MaxCap(link, RoadType)) Then
                        HourlySpeeds(link, sc, h) = FreeFlowSpeeds(sc + 1, 2)
                    ElseIf RoadTypeFlows(link, RoadType, h) <= MaxCap(link, RoadType) Then
                        'otherwise if it is in between the free flow capacity and the maximum capacity then use the speed calculator
                        'because this is the first year set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(link, RoadType)
                        FlowNew = RoadTypeFlows(link, RoadType, h)
                        SpeedOld = FreeFlowSpeeds(sc + 1, 2)
                        Call SpeedCalc()
                        HourlySpeeds(link, sc, h) = SpeedNew
                    Else
                        'TODO - Had to cut this out as it was crashing when the errors got over 48 in number - should replace this with database log
                        'this shouldn't happen in the base year, as we should already have reset the maximum capacity variable in the start flows sub, so write error to log file and exit model
                        'logarray(logNum, 0) = "ERROR in interzonal road model - maximum capacity exceeded in base year for Flow " & FlowID(link, 1) & ", road type " & RoadType & ", hour " & h & ". Model run terminated."
                        'logNum += 1
                        'Call WriteData("Logfile", "", logarray) 'TODO - replace with a log file save
                        Stop
                    End If
                    h += 1
                Loop
                sc += 1
                h = 0
            Loop
        Else
            'v1.3 mod moved inside the if clause as otherwise fails in 1st year if maximum capacity has been reset in base year
            MaxCap(link, 0) = ExternalValues(link, 11)
            MaxCap(link, 1) = ExternalValues(link, 12)
            MaxCap(link, 2) = ExternalValues(link, 13)
            'if it isn't the first year then we need to check if the capacity has changed
            If CapChangeNew(link, 1) = True Then

                'if capacity has changed since the previous year then need to recalculate the base speeds based on the new capacities (having already recalculated trips per lane in previous sub)
                h = 0
                'v1.3 MODIFICATION - need to split this out so that we do each road type separately
                Do While h < 24
                    'for class 0
                    If RoadTypeFlows(link, 0, h) < (FreeFlowCU * MaxCap(link, 0)) Then
                        For t = 0 To 5
                            'if traffic less than free flow capacity then adopt free flow speed
                            HourlySpeeds(link, t, h) = FreeFlowSpeeds(t + 1, 2)
                        Next
                    ElseIf RoadTypeFlows(link, 0, h) <= MaxCap(link, 0) Then
                        'otherwise if it is in between the free flow capacity and the maximum capacity then use the speed calculator
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(link, 0)
                        FlowNew = RoadTypeFlows(link, 0, h)
                        For t = 0 To 5
                            SpeedOld = FreeFlowSpeeds(t + 1, 2)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(link, t, h) = SpeedNew
                        Next
                    Else
                        'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable
                        LatentTraffic = RoadTypeFlows(link, 0, h) - MaxCap(link, 0)
                        For t = 0 To 5
                            LatentHourlyFlows(link, t, h) += (OldHourlyFlows(link, t, h) / RoadTypeFlows(link, 0, h)) * LatentTraffic
                        Next
                        'set the traffic level as equal to the road capacity
                        RoadTypeFlows(link, 0, h) = MaxCap(link, 0)
                        'then calculate the speed as before
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(link, 0)
                        FlowNew = RoadTypeFlows(link, 0, h)
                        For t = 0 To 5
                            SpeedOld = FreeFlowSpeeds(t + 1, 2)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(link, t, h) = SpeedNew
                        Next
                    End If
                    'for class 1
                    If RoadTypeFlows(link, 1, h) < (FreeFlowCU * MaxCap(link, 1)) Then
                        For t = 6 To 11
                            'if traffic less than free flow capacity then adopt free flow speed
                            HourlySpeeds(link, t, h) = FreeFlowSpeeds(t + 1, 2)
                        Next
                    ElseIf RoadTypeFlows(link, 1, h) <= MaxCap(link, 1) Then
                        'otherwise if it is in between the free flow capacity and the maximum capacity then use the speed calculator
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(link, 1)
                        FlowNew = RoadTypeFlows(link, 1, h)
                        For t = 6 To 11
                            SpeedOld = FreeFlowSpeeds(t + 1, 2)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(link, t, h) = SpeedNew
                        Next
                    Else
                        'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable
                        LatentTraffic = RoadTypeFlows(link, 1, h) - MaxCap(link, 1)
                        For t = 6 To 11
                            LatentHourlyFlows(link, t, h) += (OldHourlyFlows(link, t, h) / RoadTypeFlows(link, 1, h)) * LatentTraffic
                        Next
                        'set the traffic level as equal to the road capacity
                        RoadTypeFlows(link, 1, h) = MaxCap(link, 1)
                        'then calculate the speed as before
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(link, 1)
                        FlowNew = RoadTypeFlows(link, 1, h)
                        For t = 6 To 11
                            SpeedOld = FreeFlowSpeeds(t + 1, 2)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(link, t, h) = SpeedNew
                        Next
                    End If
                    'for class 2
                    If RoadTypeFlows(link, 2, h) < (FreeFlowCU * MaxCap(link, 2)) Then
                        For t = 12 To 19
                            'if traffic less than free flow capacity then adopt free flow speed
                            HourlySpeeds(link, t, h) = FreeFlowSpeeds(t + 1, 2)
                        Next
                    ElseIf RoadTypeFlows(link, 2, h) <= MaxCap(link, 2) Then
                        'otherwise if it is in between the free flow capacity and the maximum capacity then use the speed calculator
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(link, 2)
                        FlowNew = RoadTypeFlows(link, 2, h)
                        For t = 12 To 19
                            SpeedOld = FreeFlowSpeeds(t + 1, 2)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(link, t, h) = SpeedNew
                        Next
                    Else
                        'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable
                        LatentTraffic = RoadTypeFlows(link, 2, h) - MaxCap(link, 2)
                        For t = 12 To 19
                            LatentHourlyFlows(link, t, h) += (OldHourlyFlows(link, t, h) / RoadTypeFlows(link, 2, h)) * LatentTraffic
                        Next
                        'set the traffic level as equal to the road capacity
                        RoadTypeFlows(link, 2, h) = MaxCap(link, 2)
                        'then calculate the speed as before
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(link, 2)
                        FlowNew = RoadTypeFlows(link, 2, h)
                        For t = 12 To 19
                            SpeedOld = FreeFlowSpeeds(t + 1, 2)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(link, t, h) = SpeedNew
                        Next
                    End If
                    h += 1
                Loop


            Else
                'if capacity hasn't changed then can simply take the final set of speeds from the last set of output data
                sc = 0
                h = 0

                'for all 20 road link categories and 24 hours
                Do While sc < 20
                    Do While h < 24
                        HourlySpeeds(link, sc, h) = NewHourlySpeeds(link, sc, h)
                        h += 1
                    Loop
                    sc += 1
                    h = 0
                Loop
            End If

        End If
    End Sub

    Function AssignRoadType(ByVal sc As Integer) As Integer
        Dim RoadType As Integer

        'assigns a speed category to a road type
        Select Case sc
            Case 0, 1, 2, 3, 4, 5
                RoadType = 0

            Case 6, 7, 8, 9, 10, 11
                RoadType = 1

            Case 12, 13, 14, 15, 16, 17, 18, 19
                RoadType = 2
            Case Else
                RoadType = 3
        End Select

        Return RoadType
    End Function

    Sub SpeedCalc()
        'calculates change in speed as result of change in flow
        If VariableEl = True Then
            OldX = SpeedOld
            OldY = FlowOld
            NewY = FlowNew
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(1, 11)
                Call VarElCalc()
                SpeedRatio = VarRat
            Else
                SpeedRatio = (FlowNew / FlowOld) ^ RoadEls(1, 11)
            End If
        Else
            SpeedRatio = (FlowNew / FlowOld) ^ RoadEls(1, 11)
        End If
        SpeedNew = SpeedOld * SpeedRatio
    End Sub

    Sub HourlyFlowCalc()
        'calculates change in flows in each hour based on changes in other variables
        'this contains the main model equation
        'modification completed - speed calculations updated and latent values added in
        'now modified to include congestion charge option
        'also now includes smart logistics option for HGV traffic

        Dim ratnum As Integer
        Dim concharge(19) As Double

        'get the new costs
        ratnum = 14
        For x = 0 To 19
            For c = 0 To 24
                CostNew(x, c) = ExternalValues(link, ratnum)
            Next
            ratnum += 1
        Next

        sc = 0
        h = 0

        'calculate the individual variable ratios for passenger traffic
        'v1.3 mod
        If TripRates = True Then
            rat1 = (((CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)) * RdTripRates(0, 1)) / (Z1Pop(link, 1) + Z2Pop(link, 1))) ^ RoadEls(1, 3)
        Else
            rat1 = ((CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)) / (Z1Pop(link, 1) + Z2Pop(link, 1))) ^ RoadEls(1, 3)
        End If

        rat3 = ((CDbl(ExternalValues(link, 6)) + ExternalValues(link, 7)) / (Z1GVA(link, 1) + Z2GVA(link, 1))) ^ RoadEls(1, 4)
        'initially set speed new and speed old as equal to speed in previous year, so ratio = 1 - can be altered if desired as part of scenario
        rat5 = 1 ^ RoadEls(1, 5)

        'cost ratio now estimated in getflowratio sub

        'calculate the individual variable ratios for freight traffic
        'v1.3 mod
        If TripRates = True Then
            ratf1 = (((CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)) * RdTripRates(1, 1)) / (Z1Pop(link, 1) + Z2Pop(link, 1))) ^ RoadEls(1, 7)
        Else
            ratf1 = ((CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)) / (Z1Pop(link, 1) + Z2Pop(link, 1))) ^ RoadEls(1, 7)
        End If

        ratf3 = ((CDbl(ExternalValues(link, 6)) + ExternalValues(link, 7)) / (Z1GVA(link, 1) + Z2GVA(link, 1))) ^ RoadEls(1, 8)
        '***NOTE - if altering this elasticity will also need to alter the flow-speed iteration process as this used the rat5 variable only
        ratf5 = 1 ^ RoadEls(1, 9)
        'cost ratio now estimated in getflowratio sub

        Do While h < 24
            '***arguably we should iterate across all categories simultaneously, but this will complicate the iteration
            'do iteration for categories within road class 0

            RoadType = 0
            sc = 0
            ClassFlow = OldHourlyFlows(link, 0, h) + OldHourlyFlows(link, 1, h) + OldHourlyFlows(link, 2, h) + OldHourlyFlows(link, 3, h) + OldHourlyFlows(link, 4, h) + OldHourlyFlows(link, 5, h)
            If ClassFlow > 0 Then
                'scale hourly flows based on all ratios except that for speed, which requires iteration
                'sum as we go along to get an initial figure for the new total road class flow
                ClassFlowNew = 0
                Do While sc < 6
                    Call GetFlowRatio()
                    ScaledHourlyFlows(sc, h) = OldHourlyFlows(link, sc, h) * FlowRatio
                    ClassFlowNew += ScaledHourlyFlows(sc, h)
                    sc += 1
                Loop
                'we can assume that change in all speeds within class is proportionally the same, so only need to calculate single speed ratio
                sc = 0
                'v1.2 completed modification
                'set old speed to the initial hourly speed for first speed category to start with
                SpeedOld = HourlySpeeds(link, sc, h)
                'set speed original to equal speedold to save the original speed, which is used for calculating the hourly speeds
                SpeedOriginal = SpeedOld
                If ClassFlowNew < (FreeFlowCU * MaxCap(link, RoadType)) Then
                    'if road class flow is less than the free flow capacity then just use the free flow speed
                    FlowNew = ClassFlowNew
                    SpeedNew = SpeedOld
                ElseIf ClassFlowNew <= MaxCap(link, RoadType) Then
                    'in this case we do still call the flow-speed iterator
                    Call FlowSpeedIterate()
                Else
                    'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable 
                    LatentTraffic = ClassFlowNew - MaxCap(link, RoadType)
                    sc = 0
                    Do While sc < 6
                        LatentHourlyFlows(link, sc, h) += (OldHourlyFlows(link, sc, h) / ClassFlow) * LatentTraffic
                        sc += 1
                    Loop
                    'set the traffic level as equal to the road capacity
                    RoadTypeFlows(link, RoadType, h) = MaxCap(link, RoadType)
                    'then calculate the speed as before
                    'because capacity has changed set the old speed as the free flow speed
                    FlowOld = ClassFlow
                    FlowNew = RoadTypeFlows(link, RoadType, h)
                    Call SpeedCalc()
                End If
                'end of modification
                'set value in new hourly flows array - to equal stable flow value - and in new hourly speeds array
                'this updates the initial scaled hourly flow value by multiplying it by the ratio resulting from the iteration
                sc = 0
                Do While sc < 6
                    NewHourlyFlows(sc, h) = ScaledHourlyFlows(sc, h) * (FlowNew / ClassFlowNew)
                    NewHourlySpeeds(link, sc, h) = HourlySpeeds(link, sc, h) * (SpeedNew / SpeedOriginal)
                    sc += 1
                Loop
            Else
                Do While sc < 6
                    NewHourlyFlows(sc, h) = 0
                    NewHourlySpeeds(link, sc, h) = HourlySpeeds(link, sc, h)
                    sc += 1
                Loop

            End If
            'do iteration for categories within road class 1
            RoadType = 1
            sc = 6
            ClassFlow = OldHourlyFlows(link, 6, h) + OldHourlyFlows(link, 7, h) + OldHourlyFlows(link, 8, h) + OldHourlyFlows(link, 9, h) + OldHourlyFlows(link, 10, h) + OldHourlyFlows(link, 11, h)
            If ClassFlow > 0 Then
                'scale hourly flows based on all ratios except that for speed, which requires iteration
                'sum as we go along to get an initial figure for the new total road class flow
                ClassFlowNew = 0
                Do While sc < 12
                    Call GetFlowRatio()
                    ScaledHourlyFlows(sc, h) = OldHourlyFlows(link, sc, h) * FlowRatio
                    ClassFlowNew += ScaledHourlyFlows(sc, h)
                    sc += 1
                Loop
                'we can assume that change in all speeds within class is proportionally the same, so only need to calculate single speed ratio
                sc = 6
                'v1.2 completed modification
                'set old speed to the initial hourly speed for first speed category to start with
                SpeedOld = HourlySpeeds(link, sc, h)
                'set speed original to equal speedold to save the original speed, which is used for calculating the hourly speeds
                SpeedOriginal = SpeedOld
                If ClassFlowNew < (FreeFlowCU * MaxCap(link, RoadType)) Then
                    'if road class flow is less than the free flow capacity then just use the free flow speed
                    FlowNew = ClassFlowNew
                    SpeedNew = SpeedOld
                ElseIf ClassFlowNew <= MaxCap(link, RoadType) Then
                    'in this case we do still call the flow-speed iterator
                    Call FlowSpeedIterate()
                Else
                    'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable 
                    LatentTraffic = ClassFlowNew - MaxCap(link, RoadType)
                    sc = 6
                    Do While sc < 12
                        LatentHourlyFlows(link, sc, h) += (OldHourlyFlows(link, sc, h) / ClassFlow) * LatentTraffic
                        sc += 1
                    Loop
                    'set the traffic level as equal to the road capacity
                    RoadTypeFlows(link, RoadType, h) = MaxCap(link, RoadType)
                    'then calculate the speed as before
                    'because capacity has changed set the old speed as the free flow speed
                    FlowOld = ClassFlow
                    FlowNew = RoadTypeFlows(link, RoadType, h)
                    Call SpeedCalc()
                End If
                'end of modification
                'set value in new hourly flows array - to equal stable flow value - and in new hourly speeds array
                'this updates the initial scaled hourly flow value by multiplying it by the ratio resulting from the iteration
                sc = 6
                Do While sc < 12
                    NewHourlyFlows(sc, h) = ScaledHourlyFlows(sc, h) * (FlowNew / ClassFlowNew)
                    NewHourlySpeeds(link, sc, h) = HourlySpeeds(link, sc, h) * (SpeedNew / SpeedOriginal)
                    sc += 1
                Loop
            Else
                Do While sc < 12
                    NewHourlyFlows(sc, h) = 0
                    NewHourlySpeeds(link, sc, h) = HourlySpeeds(link, sc, h)
                    sc += 1
                Loop
            End If
            'do iteration for categories within road class 2
            RoadType = 2
            sc = 12
            ClassFlow = OldHourlyFlows(link, 12, h) + OldHourlyFlows(link, 13, h) + OldHourlyFlows(link, 14, h) + OldHourlyFlows(link, 15, h) + OldHourlyFlows(link, 16, h) + OldHourlyFlows(link, 17, h) + OldHourlyFlows(link, 18, h) + OldHourlyFlows(link, 19, h)
            If ClassFlow > 0 Then
                'scale hourly flows based on all ratios except that for speed, which requires iteration
                'sum as we go along to get an initial figure for the new total road class flow
                ClassFlowNew = 0
                Do While sc < 20
                    Call GetFlowRatio()
                    ScaledHourlyFlows(sc, h) = OldHourlyFlows(link, sc, h) * FlowRatio
                    ClassFlowNew += ScaledHourlyFlows(sc, h)
                    sc += 1
                Loop
                'we can assume that change in all speeds within class is proportionally the same, so only need to calculate single ratio
                sc = 12
                'v1.2 completed modification
                'set old speed to the initial hourly speed for first speed category to start with
                SpeedOld = HourlySpeeds(link, sc, h)
                'set speed original to equal speedold to save the original speed, which is used for calculating the hourly speeds
                SpeedOriginal = SpeedOld
                If ClassFlowNew < (FreeFlowCU * MaxCap(link, RoadType)) Then
                    'if road class flow is less than the free flow capacity then just use the free flow speed
                    FlowNew = ClassFlowNew
                    SpeedNew = SpeedOld
                ElseIf ClassFlowNew <= MaxCap(link, RoadType) Then
                    'in this case we do still call the flow-speed iterator
                    Call FlowSpeedIterate()
                Else
                    'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable 
                    LatentTraffic = ClassFlowNew - MaxCap(link, RoadType)
                    sc = 12
                    Do While sc < 20
                        LatentHourlyFlows(link, sc, h) += (OldHourlyFlows(link, sc, h) / ClassFlow) * LatentTraffic
                        sc += 1
                    Loop
                    'set the traffic level as equal to the road capacity
                    RoadTypeFlows(link, RoadType, h) = MaxCap(link, RoadType)
                    'then calculate the speed as before
                    'because capacity has changed set the old speed as the free flow speed
                    FlowOld = ClassFlow
                    FlowNew = RoadTypeFlows(link, RoadType, h)
                    Call SpeedCalc()
                End If
                'end of modification
                'set value in new hourly flows array - to equal stable flow value - and in new hourly speeds array
                'this updates the initial scaled hourly flow value by multiplying it by the ratio resulting from the iteration
                sc = 12
                Do While sc < 20
                    NewHourlyFlows(sc, h) = ScaledHourlyFlows(sc, h) * (FlowNew / ClassFlowNew)
                    NewHourlySpeeds(link, sc, h) = HourlySpeeds(link, sc, h) * (SpeedNew / SpeedOriginal)
                    sc += 1
                Loop
            Else
                Do While sc < 20
                    NewHourlyFlows(sc, h) = 0
                    NewHourlySpeeds(link, sc, h) = HourlySpeeds(link, sc, h)
                    sc += 1
                Loop
            End If
            'if using smart logistics then need to scale the HGV traffic
            If SmartFrt = True Then
                If g_modelRunYear > SmFrtIntro Then
                    'if year is after the introduction year and before or equal to the year when the final effect is realised then need to scale HGV traffic
                    If g_modelRunYear <= SmFrtIntro + SmFrtYears Then
                        NewHourlyFlows(4, h) = NewHourlyFlows(4, h) * (1 - (SmFrtPer / SmFrtYears))
                        NewHourlyFlows(5, h) = NewHourlyFlows(5, h) * (1 - (SmFrtPer / SmFrtYears))
                        NewHourlyFlows(10, h) = NewHourlyFlows(10, h) * (1 - (SmFrtPer / SmFrtYears))
                        NewHourlyFlows(11, h) = NewHourlyFlows(11, h) * (1 - (SmFrtPer / SmFrtYears))
                        For x = 16 To 19
                            NewHourlyFlows(x, h) = NewHourlyFlows(x, h) * (1 - (SmFrtPer / SmFrtYears))
                        Next
                        'also need to scale the equivalent latent traffic by the same amount
                        LatentHourlyFlows(link, 4, h) = LatentHourlyFlows(link, 4, h) * (1 - (SmFrtPer / SmFrtYears))
                        LatentHourlyFlows(link, 5, h) = LatentHourlyFlows(link, 5, h) * (1 - (SmFrtPer / SmFrtYears))
                        LatentHourlyFlows(link, 10, h) = LatentHourlyFlows(link, 10, h) * (1 - (SmFrtPer / SmFrtYears))
                        LatentHourlyFlows(link, 11, h) = LatentHourlyFlows(link, 11, h) * (1 - (SmFrtPer / SmFrtYears))
                        For x = 16 To 19
                            LatentHourlyFlows(link, x, h) = LatentHourlyFlows(link, x, h) * (1 - (SmFrtPer / SmFrtYears))
                        Next
                    End If
                End If
            End If

            h += 1
        Loop

    End Sub

    Sub GetFlowRatio()
        'now includes congestion charge element
        '***can probably speed up this sub by moving everything except the first loop within the select case at the end
        Dim conchargeprop As Double

        'add in congestion charge if using this - have to do this here because it will vary by the hour
        If CongestionCharge = True Then
            'check if we are in a year after the charge has started
            If g_modelRunYear >= ConChargeYear Then
                If ClassFlowNew < (FreeFlowCU * MaxCap(link, RoadType)) Then
                    conchargeprop = 0
                Else
                    conchargeprop = ClassFlowNew / MaxCap(link, RoadType)
                    If conchargeprop > 0.9 Then
                        conchargeprop = conchargeprop
                    End If
                End If
                'use select case to set charges, as no charge set for buses
                Select Case sc
                    Case 0, 1, 2, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 16, 17, 18, 19
                        ChargeNew(sc, h) = StandingCosts(link, sc) * ConChargePer * (conchargeprop ^ 2)
                    Case 3, 9, 15
                        ChargeNew(sc, h) = 0
                End Select
            Else
                ChargeNew(sc, h) = 0
            End If
        Else
            ChargeNew(sc, h) = 0
        End If

        'have to build in variable elasticities here - because the x value (and hence the variable elasticity) will vary depending on the speed category and hour
        If VariableEl = True Then
            OldX = OldHourlyFlows(link, sc, h)
            'pop ratio
            OldY = Z1Pop(link, 1) + Z2Pop(link, 1)
            If TripRates = True Then
                NewY = (CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)) * RdTripRates(1, 1)
            Else
                NewY = CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)
            End If
            NewY = CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(1, 3)
                Call VarElCalc()
                rat1 = VarRat
            End If
            'gva ratio
            OldY = Z1GVA(link, 1) + Z2GVA(link, 1)
            NewY = CDbl(ExternalValues(link, 6)) + ExternalValues(link, 7)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(1, 4)
                Call VarElCalc()
                rat3 = VarRat
            End If
            'don't need to include speed ratio as this is held at 1 initially
            'cost ratios
            'modification - don't need to calculate them all each time, only the one for the speed category we are currently looking at
            OldY = CostOld(link, sc, h) + ChargeOld(link, sc, h)
            NewY = CostNew(sc, h) + ChargeNew(sc, h)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(1, 4)
                Call VarElCalc()
                rat6(sc) = VarRat
            Else
                rat6(sc) = (NewY / OldY) ^ RoadEls(1, 6)
            End If
            'freight pop ratio
            OldY = Z1Pop(link, 1) + Z2Pop(link, 1)
            If TripRates = True Then
                NewY = (CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)) * RdTripRates(1, 1)
            Else
                NewY = CDbl(ExternalValues(link, 4)) + ExternalValues(link, 5)
            End If
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(1, 7)
                Call VarElCalc()
                ratf1 = VarRat
            End If
            'freight gva ratio
            OldY = Z1GVA(link, 1) + Z2GVA(link, 1)
            NewY = CDbl(ExternalValues(link, 6)) + ExternalValues(link, 7)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(1, 8)
                Call VarElCalc()
                ratf3 = VarRat
            End If
            'don't need to include speed ratio as this is held at 1 initially
            'freight cost ratios
            OldY = CostOld(link, sc, h) + ChargeOld(link, sc, h)
            NewY = CostNew(sc, h) + ChargeNew(sc, h)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(1, 10)
                Call VarElCalc()
                ratf6(sc) = VarRat
            Else
                ratf6(sc) = (CostNew(sc, h) / CostOld(link, sc, h)) ^ RoadEls(1, 10)
            End If
        Else
            'still need to recalculate ratio if using congestion charging and to calculate cost ratio if not (latter now moved from hourly flow calc sub
            If CongestionCharge = True Then
                OldY = CostOld(link, sc, h) + ChargeOld(link, sc, h)
                NewY = CostNew(sc, h) + ChargeNew(sc, h)
                rat6(sc) = (NewY / OldY) ^ RoadEls(1, 6)
                OldY = CostOld(link, sc, h) + ChargeOld(link, sc, h)
                NewY = CostNew(sc, h) + ChargeNew(sc, h)
                ratf6(sc) = (NewY / OldY) ^ RoadEls(1, 10)
            Else
                rat6(sc) = (CostNew(sc, h) / CostOld(link, sc, h)) ^ RoadEls(1, 6)
                ratf6(sc) = (CostNew(sc, h) / CostOld(link, sc, h)) ^ RoadEls(1, 10)
            End If
        End If

        'calculate the flow ratio
        PFlowRatio(sc) = rat1 * rat3 * rat5 * rat6(sc)
        FFlowRatio(sc) = ratf1 * ratf3 * ratf5 * ratf6(sc)

        Select Case sc
            Case 0, 1, 3, 6, 7, 9, 12, 13, 15
                FlowRatio = PFlowRatio(sc)
            Case 2, 4, 5, 8, 10, 11, 14, 16, 17, 18, 19
                FlowRatio = FFlowRatio(sc)
        End Select
    End Sub

    Sub VarElCalc()
        Dim alpha, beta As Double
        Dim xnew As Double

        'v1.4 modification to deal with occasions when flow is zero
        If OldX > 0 Then
            alpha = OldX / Math.Exp(OldEl)
            beta = (Math.Log(OldX / alpha)) / OldY
            xnew = alpha * Math.Exp(beta * NewY)
            VarRat = xnew / OldX
        Else
            VarRat = 1
        End If

    End Sub

    Sub FlowSpeedIterate()
        'This variable gives the ratio between the speeds and is used to check if the values have converged
        Dim SpeedRat As Double
        Dim z As Long

        'set speed ratio to 0 to start with
        SpeedRat = 0

        FlowOld = ClassFlow
        FlowNew = ClassFlowNew
        z = 1
        Do Until SpeedRat >= 0.99 And SpeedRat <= 1.01
            'this iterates between the speed and flow ratios until convergence
            'if new flow is higher than free flow capacity then calculate the new speed and speed ratio
            Call SpeedCalc()
            'variable elasticities now added
            If VariableEl = True Then
                OldX = FlowOld
                OldY = SpeedOld
                NewY = SpeedNew
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = 0.41
                    Call VarElCalc()
                    rat5 = VarRat
                Else
                    rat5 = (SpeedNew / SpeedOld) ^ 0.41
                End If
            Else
                rat5 = (SpeedNew / SpeedOld) ^ 0.41
            End If
            'then recalculate the flow
            FlowOld = FlowNew
            FlowNew = FlowOld * rat5
            SpeedRat = SpeedNew / SpeedOld
            'resets speed old variable to be equal to speed new variable from last iteration
            SpeedOld = SpeedNew
            'Debugger - checks if stuck in loop and writes to log
            z += 1
            If z > 1000 Then
                logarray(logNum, 0) = "ERROR in Road Link Module: Flow" & FlowID(link, 1) & " Year" & g_modelRunYear & " Road Type " & RoadType & " speed and flow failed to converge after 1000 iterations"
                logNum += 1
                Call WriteData("Logfile", "", logarray)
                Exit Do
            Else
            End If
        Loop
        'recalculate speed using the final new flow value
        Call SpeedCalc()
        'End If

    End Sub

    Sub TotalFlow()
        'sums the hourly flows from each of the speed categories to give new total flows within each category and an overall total flow
        'also calculates average speeds for each of the categories over the day
        'v1.2 modification completed - now calculates total latent traffic for each road type and number of hours when each road type is full
        'v1.4 modification - now also calculates average cost for each road type

        Dim latenttrips As Double
        Dim costaggregate As Double

        TotalFlowNew = 0
        MeanSpeedNew = 0
        MwayFlowNew = 0
        MWaySpdNew = 0
        DualFlowNew = 0
        DualSpdNew = 0
        SingFlowNew = 0
        SingSpdNew = 0
        MWayLatFlowNew = 0
        DualLatFlowNew = 0
        SingLatFlowNew = 0
        MFullHrs = 0
        DFullHrs = 0
        SFullHrs = 0
        ReDim SpeedCatFlowsNew(20)
        ReDim SpeedCatSpeedsNew(20)
        ReDim LatentFlows(20)
        sc = 0
        h = 0
        Do While sc < 20
            Do While h < 24
                TotalFlowNew = TotalFlowNew + NewHourlyFlows(sc, h)
                SpeedCatFlowsNew(sc) = SpeedCatFlowsNew(sc) + NewHourlyFlows(sc, h)
                SpeedCatSpeedsNew(sc) = SpeedCatSpeedsNew(sc) + (NewHourlyFlows(sc, h) * NewHourlySpeeds(link, sc, h))
                LatentFlows(sc) = LatentFlows(sc) + LatentHourlyFlows(link, sc, h)
                h += 1
            Loop
            'only calculate speed if flow is greater than zero, otherwise set it to the last new hourly speed (as all will be free flow)
            If SpeedCatFlowsNew(sc) > 0 Then
                SpeedCatSpeedsNew(sc) = SpeedCatSpeedsNew(sc) / SpeedCatFlowsNew(sc)
                MeanSpeedNew = MeanSpeedNew + (SpeedCatFlowsNew(sc) * SpeedCatSpeedsNew(sc))
            Else
                SpeedCatSpeedsNew(sc) = NewHourlySpeeds(link, sc, h)
            End If
            h = 0
            sc += 1
        Loop
        'calculate motorway flows and speeds
        'v1.4 also costs
        sc = 0
        costaggregate = 0
        Do While sc < 6
            MwayFlowNew = MwayFlowNew + SpeedCatFlowsNew(sc)
            MWaySpdNew = MWaySpdNew + (SpeedCatFlowsNew(sc) * SpeedCatSpeedsNew(sc))
            MWayLatFlowNew += LatentFlows(sc)
            h = 0
            Do While h < 24
                costaggregate += (NewHourlyFlows(sc, h) * (CostNew(sc, h) + ChargeNew(sc, h)))
                h += 1
            Loop
            sc += 1
        Loop
        If MwayFlowNew > 0 Then
            MWaySpdNew = MWaySpdNew / MwayFlowNew
            MeanCostNew(0) = costaggregate / MwayFlowNew
        Else
            MWaySpdNew = 0
            MeanCostNew(0) = 0
        End If
        'v1.4 modification multiply by number of lanes to give total flow across all lanes
        MwayFlowNew = MwayFlowNew * RoadTypeLanes(link, 0)
        'calculate dual flows and speeds
        'v1.4 also costs
        sc = 6
        costaggregate = 0
        Do While sc < 12
            DualFlowNew = DualFlowNew + SpeedCatFlowsNew(sc)
            DualSpdNew = DualSpdNew + (SpeedCatFlowsNew(sc) * SpeedCatSpeedsNew(sc))
            DualLatFlowNew += LatentFlows(sc)
            h = 0
            Do While h < 24
                costaggregate += (NewHourlyFlows(sc, h) * (CostNew(sc, h) + ChargeNew(sc, h)))
                h += 1
            Loop
            sc += 1
        Loop
        If DualFlowNew > 0 Then
            DualSpdNew = DualSpdNew / DualFlowNew
            MeanCostNew(1) = costaggregate / DualFlowNew
        Else
            DualSpdNew = 0
            MeanCostNew(1) = 0
        End If
        'v1.4 modification multiply by number of lanes to give total flow across all lanes
        DualFlowNew = DualFlowNew * RoadTypeLanes(link, 1)
        'calculate single flows and speeds
        'v1.4 also costs
        sc = 12
        costaggregate = 0
        Do While sc < 20
            SingFlowNew = SingFlowNew + SpeedCatFlowsNew(sc)
            SingSpdNew = SingSpdNew + (SpeedCatFlowsNew(sc) * SpeedCatSpeedsNew(sc))
            SingLatFlowNew += LatentFlows(sc)
            h = 0
            Do While h < 24
                costaggregate += (NewHourlyFlows(sc, h) * (CostNew(sc, h) + ChargeNew(sc, h)))
                h += 1
            Loop
            sc += 1
        Loop
        If SingFlowNew > 0 Then
            SingSpdNew = SingSpdNew / SingFlowNew
            MeanCostNew(2) = costaggregate / SingFlowNew
        Else
            SingSpdNew = 0
            MeanCostNew(2) = 0
        End If
        'v1.4 modification multiply by number of lanes to give total flow across all lanes
        SingFlowNew = SingFlowNew * RoadTypeLanes(link, 2)
        'calculate overall mean speed
        If TotalFlowNew > 0 Then
            MeanSpeedNew = MeanSpeedNew / TotalFlowNew
        Else
        End If
        'v1.4 modification multiply by number of lanes to give total flow across all lanes - and move it because otherwise assuming the same flow level on all lanes
        TotalFlowNew = MwayFlowNew + DualFlowNew + SingFlowNew
        '**end mod
        'v1.2 modification - calculate number of congested hours
        h = 0
        Do While h < 24
            sc = 0
            latenttrips = 0
            Do Until sc = 6
                latenttrips += LatentHourlyFlows(link, sc, h)
                sc += 1
            Loop
            If latenttrips > 0 Then
                MFullHrs += 1
            End If
            latenttrips = 0
            Do Until sc = 12
                latenttrips += LatentHourlyFlows(link, sc, h)
                sc += 1
            Loop
            If latenttrips > 0 Then
                DFullHrs += 1
            End If
            latenttrips = 0
            Do Until sc = 20
                latenttrips += LatentHourlyFlows(link, sc, h)
                sc += 1
            Loop
            If latenttrips > 0 Then
                SFullHrs += 1
            End If
            h += 1
        Loop
    End Sub
    Sub WriteOutputRow()
        Dim cu As Double
        'Dim newcapstring As String
        Dim i As Integer
        Dim hrow As Integer

        'write to output array
        OutputArray(link, 0) = g_modelRunID
        OutputArray(link, 1) = FlowID(link, 1)
        OutputArray(link, 2) = g_modelRunYear
        OutputArray(link, 3) = TotalFlowNew
        OutputArray(link, 4) = MeanSpeedNew
        OutputArray(link, 5) = MwayFlowNew
        OutputArray(link, 6) = DualFlowNew
        OutputArray(link, 7) = SingFlowNew
        OutputArray(link, 8) = MWaySpdNew
        OutputArray(link, 9) = DualSpdNew
        OutputArray(link, 10) = SingSpdNew
        sc = 0
        Do While sc < 20
            OutputArray(link, 11 + sc) = SpeedCatFlowsNew(sc)
            sc += 1
        Loop
        sc = 0
        Do While sc < 20
            OutputArray(link, 31 + sc) = SpeedCatSpeedsNew(sc)
            sc += 1
        Loop
        OutputArray(link, 51) = MWayLatFlowNew
        OutputArray(link, 52) = DualLatFlowNew
        OutputArray(link, 53) = SingLatFlowNew
        OutputArray(link, 54) = MFullHrs
        OutputArray(link, 55) = DFullHrs
        OutputArray(link, 56) = SFullHrs
        OutputArray(link, 57) = MeanCostNew(0)
        OutputArray(link, 58) = MeanCostNew(1)
        OutputArray(link, 59) = MeanCostNew(2)

        'update variables
        ReDim RoadTypeFlows(291, 2, 23)
        sc = 0
        Do While sc < 20
            SpeedCatFlows(link, sc) = SpeedCatFlowsNew(sc)
            'v1.2 modification - now also updates hourly flows
            'v1.2 additional modification - updates road type flows too
            RoadType = AssignRoadType(sc)
            h = 0
            Do Until h > 23
                OldHourlyFlows(link, sc, h) = NewHourlyFlows(sc, h)
                RoadTypeFlows(link, RoadType, h) = RoadTypeFlows(link, RoadType, h) + OldHourlyFlows(link, sc, h)
                'now updates charge variable too
                If CongestionCharge = True Then
                    ChargeOld(link, sc, h) = ChargeNew(sc, h)
                End If
                h += 1
            Loop
            sc += 1
        Loop
        'update population, gva and cost variables based on values from external variables input file
        Z1Pop(link, 1) = ExternalValues(link, 4)
        Z2Pop(link, 1) = ExternalValues(link, 5)
        Z1GVA(link, 1) = ExternalValues(link, 6)
        Z2GVA(link, 1) = ExternalValues(link, 7)
        'new
        For x = 0 To 19
            For c = 0 To 24
                CostOld(link, x, c) = CostNew(x, c)
            Next
        Next
        RoadTypeLanesNew(link, 0) = CDbl(ExternalValues(link, 8)) + AddedLanes(link, 0)
        RoadTypeLanesNew(link, 1) = CDbl(ExternalValues(link, 9)) + AddedLanes(link, 1)
        RoadTypeLanesNew(link, 2) = CDbl(ExternalValues(link, 10)) + AddedLanes(link, 2)
        'v1.4 blank mean cost variables
        MeanCostNew(0) = 0
        MeanCostNew(1) = 0
        MeanCostNew(2) = 0
        'if building capacity then check if new capacity is needed
        If BuildInfra = True Then
            'check motorways
            If RoadTypeLanesNew(link, 0) > 0 Then
                h = 0
                Do While h < 24
                    cu = RoadTypeFlows(link, 0, h) / MaxCap(link, 0)
                    If cu >= CUCritValue Then
                        'add 2 lanes if necessary
                        RoadTypeLanesNew(link, 0) += 2
                        AddedLanes(link, 0) += 2
                        'write details to output file
                        NewCapArray(NewCapNum, 0) = FlowID(link, 1)
                        NewCapArray(NewCapNum, 1) = (g_modelRunYear + 1)
                        NewCapArray(NewCapNum, 2) = 0
                        NewCapArray(NewCapNum, 3) = 2
                        NewCapNum += 1
                        Exit Do
                    End If
                    h += 1
                Loop
            End If
            'check dual carriageways
            If RoadTypeLanesNew(link, 1) > 0 Then
                h = 0
                Do While h < 24
                    cu = RoadTypeFlows(link, 1, h) / MaxCap(link, 1)
                    If cu >= CUCritValue Then
                        'add 2 lanes if necessary
                        RoadTypeLanesNew(link, 1) += 2
                        AddedLanes(link, 1) += 2
                        'write details to output file
                        NewCapArray(NewCapNum, 0) = FlowID(link, 1)
                        NewCapArray(NewCapNum, 1) = (g_modelRunYear + 1)
                        NewCapArray(NewCapNum, 2) = 1
                        NewCapArray(NewCapNum, 3) = 2
                        NewCapNum += 1
                        Exit Do
                    End If
                    h += 1
                Loop
            End If
            'check single carriageways
            If RoadTypeLanesNew(link, 2) > 0 Then
                h = 0
                Do While h < 24
                    cu = RoadTypeFlows(link, 2, h) / MaxCap(link, 2)
                    If cu >= CUCritValue Then
                        'add 2 lanes if necessary
                        RoadTypeLanesNew(link, 2) += 2
                        AddedLanes(link, 2) += 2
                        'write details to output file
                        NewCapArray(NewCapNum, 0) = FlowID(link, 1)
                        NewCapArray(NewCapNum, 1) = (g_modelRunYear + 1)
                        NewCapArray(NewCapNum, 2) = 2
                        NewCapArray(NewCapNum, 3) = 2
                        NewCapNum += 1
                        Exit Do
                    End If
                    h += 1
                Loop
            End If
        End If

        'write to Temp Annual array
        TempAnArray(link, 0) = g_modelRunID
        TempAnArray(link, 1) = g_modelRunYear
        TempAnArray(link, 2) = FlowID(link, 1)
        TempAnArray(link, 3) = RoadTypeLanes(link, 0)
        TempAnArray(link, 4) = RoadTypeLanes(link, 1)
        TempAnArray(link, 5) = RoadTypeLanes(link, 2)
        TempAnArray(link, 6) = RoadTypeLanesNew(link, 0)
        TempAnArray(link, 7) = RoadTypeLanesNew(link, 1)
        TempAnArray(link, 8) = RoadTypeLanesNew(link, 2)
        For x = 0 To 19
            TempAnArray(link, 9 + x) = SpeedCatFlows(link, x)
        Next
        TempAnArray(link, 29) = MaxCap(link, 0)
        TempAnArray(link, 30) = MaxCap(link, 1)
        TempAnArray(link, 31) = MaxCap(link, 2)
        TempAnArray(link, 32) = AddedLanes(link, 0)
        TempAnArray(link, 33) = AddedLanes(link, 1)
        TempAnArray(link, 34) = AddedLanes(link, 2)

        'Write to the Temp Hourly Array
        'Loop through the hourly data for each roadtype (RoadType) and speed capacity (sc)
        h = 0
        Do Until h > 23
            'work out the row number for the hourly data
            hrow = (link - 1) * 24 + h + 1
            'write to TempFlow array
            TempHArray(hrow, 0) = g_modelRunID
            TempHArray(hrow, 1) = g_modelRunYear
            TempHArray(hrow, 2) = FlowID(link, 1)
            TempHArray(hrow, 3) = h + 1
            TempHArray(hrow, 4) = RoadTypeFlows(link, 0, h)
            TempHArray(hrow, 5) = RoadTypeFlows(link, 1, h)
            TempHArray(hrow, 6) = RoadTypeFlows(link, 2, h)
            i = 7
            sc = 0
            Do While sc < 20
                RoadType = AssignRoadType(sc)
                TempHArray(hrow, i) = OldHourlyFlows(link, sc, h)
                i += 1
                TempHArray(hrow, i) = ChargeOld(link, sc, h)
                i += 1
                TempHArray(hrow, i) = LatentHourlyFlows(link, sc, h)
                i += 1
                TempHArray(hrow, i) = NewHourlySpeeds(link, sc, h)
                i += 1

                sc += 1
            Loop

            h += 1
        Loop


    End Sub

    Sub CapChange()

        'get old lane values
        TotalLanesOld = RoadTypeLanes(link, 0) + RoadTypeLanes(link, 1) + RoadTypeLanes(link, 2)

        'get lane values for current year
        TotalLanesNew = RoadTypeLanesNew(link, 0) + RoadTypeLanesNew(link, 1) + RoadTypeLanesNew(link, 2)

        'compare lanes in previous year with lanes in current year
        If TotalLanesOld = TotalLanesNew Then
            CapChangeNew(link, 1) = False
        Else
            CapChangeNew(link, 1) = True
        End If

    End Sub

    Sub Year2010()
        Call ReadData("RoadLink", "Input", InputArray, g_modelRunYear)

        'read initial data and write to output table as the 2010 result
        link = 1

        Do Until link > 291
            'TODO complete the output of road link for year 2010
            OutputArray(link, 0) = g_modelRunID
            OutputArray(link, 1) = InputArray(link, 1)
            OutputArray(link, 2) = g_modelRunYear
            OutputArray(link, 3) = InputArray(link, 7)
            OutputArray(link, 4) = 50
            OutputArray(link, 5) = 0
            OutputArray(link, 6) = 0
            OutputArray(link, 7) = 0
            OutputArray(link, 8) = 0
            OutputArray(link, 9) = 0
            OutputArray(link, 10) = 0

            For i = 0 To 19
                OutputArray(link, 11 + i) = InputArray(link, 8 + i)
            Next

            link += 1
        Loop

        Call WriteData("RoadLink", "Output", OutputArray, , False)

    End Sub

End Module
