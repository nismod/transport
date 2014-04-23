Module RoadModel1pt4
    'This version is complete, and dependent on module FullCDAM for file paths.  It also now allows elasticities to vary over time.
    'version 1.2 has a revised corrected speed-flow relationship, and incorporates latent demand variables etc
    'now also includes differential costs for different vehicle types
    'now also has the option to build additional infrastructure if capacity exceeds a certain level
    'now includes a congestion charging option
    'v1.3 corrects the procedure for dealing with latent demand when capacity is added
    'v1.3 also includes smart logistics option for hgv traffic
    'now also includes variable trip rate option
    'v1.4 replaces CostNew(sc) with CostNew(sc,h)

    Dim RoadInputData As IO.FileStream
    Dim ri As IO.StreamReader
    Dim RoadOutputData As IO.FileStream
    Dim ro As IO.StreamWriter
    Dim ExtInput As IO.FileStream
    Dim ev As IO.StreamReader
    Dim RoadElasticities As IO.FileStream
    Dim re As IO.StreamReader
    Dim RoadLinkNewCap As IO.FileStream
    Dim rlnc As IO.StreamWriter
    Dim InputFlow As String
    Dim FlowDetails() As String
    Dim FlowID As Long
    Dim Zone1 As String
    Dim Zone2 As String
    'SpeedCatFlows is an array containing the flows in all the speed categories
    Dim SpeedCatFlows(20) As Double
    Dim Z1Pop As Long
    Dim Z2Pop As Long
    Dim Z1GVA As Long
    Dim Z2GVA As Long
    Dim CostOld(19, 24), CostNew(19, 24) As Double
    Dim MStartFlow() As Long
    Dim DStartFlow() As Long
    Dim SStartFlow() As Long
    Dim HourProportions(24) As Double
    Dim TimeProfile() As String
    'HourlyFlows is a two-dimensional array containing the hourly flows in all the speed categories
    'modification this has now been renamed 'OldHourlyFlows'
    'HourlySpeeds is a two-dimensional array containing the hourly speeds for all the speed categories
    Dim OldHourlyFlows(20, 24) As Double
    Dim HourlySpeeds(20, 24) As Double
    Dim RoadTypeFlows(2, 23) As Double
    Dim RoadTypeFlowsNew As Double
    Dim sc As Byte
    Dim h As Byte
    Dim FreeFlowSpeeds() As String
    Dim MaxCap(2) As String
    Dim SpeedNew As Double
    Dim SpeedOld As Double
    Dim SpeedOriginal As Double
    Dim FlowOld As Double
    Dim FlowNew As Double
    Dim SpeedRatio As Double
    Dim PFlowRatio(19) As Double
    Dim FFlowRatio(19) As Double
    Dim FlowRatio As Double
    Dim ExternalValues(31, 90) As Double
    Dim YearNum As Long
    Dim ScaledHourlyFlows(20, 24) As Double
    Dim NewHourlyFlows(20, 24) As Double
    Dim NewHourlySpeeds(20, 24) As Double
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
    Dim TotalLanesOriginal As Long
    Dim RoadTypeLanes(2) As Integer
    Dim RoadTypeLanesNew(2) As Integer
    Dim TotalLanesOld As Long
    Dim TotalLanesNew As Long
    Dim CapChangeNew As Boolean
    Dim RoadEls(9, 90) As Double
    Dim FreeFlowCU As Double
    'v1.2 this is a new variable, storing latent demand
    Dim LatentHourlyFlows(20, 24) As Double
    'v1.2 this is also new, storing total latent demand for each speed category
    Dim LatentFlows(20) As Double
    'this is just a temporary holding variable
    Dim LatentTraffic As Double
    Dim MWayLatFlowNew, DualLatFlowNew, SingLatFlowNew As Double
    Dim MFullHrs, DFullHrs, SFullHrs As Long
    Dim AddedLanes(2) As Integer
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim StandingCosts(19) As Double
    Dim ChargeNew(19, 24), ChargeOld(19, 24) As Double
    Dim RdTripRates(1, 90) As Double
    Dim MeanCostNew(2) As Double

    Public Sub RoadLinkMain()
        Dim Header As Boolean

        Call SetFiles()

        Header = False

        'v1.2 modification: for the moment we are saying that if capacity utilisation is less than 0.25 then traffic flows at free flow speed - but may wish to alter this
        FreeFlowCU = 0.25

        Do
            'loop through all the flows in the input file
            InputFlow = ri.ReadLine
            'check if first line
            If Header = False Then
                Header = True
                'load the daily travel proportions
                Call DailyProfile()
                'load the elasticities
                Call ReadRoadEls()

            ElseIf InputFlow Is Nothing Then
                'check if at end of file
                Exit Do

            Else
                'modification v1.2 - input file replaced by internally specified values - because these have to be altered for some links
                'further modification - these are now specified in the input file (and therefore any alterations where base usage exceeds the theoretical maximum have to be made in the input file)

                'reset the latent hourly flows variable, to clean out the previous lot of data
                ReDim LatentHourlyFlows(20, 24)

                'update the input variables
                Call LoadInputRow()

                'get external variable values
                Call GetExternalValues()

                'set YearNum equal to one
                YearNum = 1

                'set capacity changed checker to false
                CapChangeNew = False

                'v2.1 modification completed - hourly flow calculation now moved to here, as only want to split it based on the daily profile once
                'calculate the starting hourly flows
                Call StartFlows()

                'Loop through flow and speed calculation modules for each year until end of period

                Do Until YearNum > 90

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

                    'write the flows to the output file
                    Call WriteOutputRow()

                    'write the flows to temp file
                    Call WriteInputFile()

                    'read the flows from the temp file and delete the temp file
                    Call ReadInputFile()

                    'reset the input flow sizes and speeds for the next year
                    Call UpdateInputVars()

                    YearNum += 1
                Loop
            End If
        Loop

        'Close input and output files
        ri.Close()
        ro.Close()
        ev.Close()
        re.Close()
        If BuildInfra = True Then
            rlnc.Close()
        End If

    End Sub

    Sub SetFiles()
        Dim DayTripProfile As IO.FileStream
        Dim tp As IO.StreamReader
        Dim row As String
        Dim SpeedInput As IO.FileStream
        Dim si As IO.StreamReader
        Dim rls As IO.StreamReader
        Dim stratarray() As String

        RoadInputData = New IO.FileStream(DirPath & "RoadInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        ri = New IO.StreamReader(RoadInputData, System.Text.Encoding.Default)

        DayTripProfile = New IO.FileStream(DirPath & "DailyTripProfile.csv", IO.FileMode.Open, IO.FileAccess.Read)
        tp = New IO.StreamReader(DayTripProfile, System.Text.Encoding.Default)
        row = tp.ReadLine
        TimeProfile = Split(row, ",")
        tp.Close()

        SpeedInput = New IO.FileStream(DirPath & "FreeFlowSpeedsv0.7.csv", IO.FileMode.Open, IO.FileAccess.Read)
        si = New IO.StreamReader(SpeedInput, System.Text.Encoding.Default)
        row = si.ReadLine
        FreeFlowSpeeds = Split(row, ",")
        si.Close()

        If UpdateExtVars = True Then
            If NewRdLCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If
        ExtInput = New IO.FileStream(DirPath & EVFilePrefix & "ExternalVariables" & EVFileSuffix & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        ev = New IO.StreamReader(ExtInput, System.Text.Encoding.Default)
        'read header row
        row = ev.ReadLine
        'then leave this file for the external variables call in the main sub - will read 90 rows each time it goes through the loop

        RoadOutputData = New IO.FileStream(DirPath & FilePrefix & "RoadOutputFlows.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        ro = New IO.StreamWriter(RoadOutputData, System.Text.Encoding.Default)
        'write header row
        OutputRow = "FlowID,Yeary,PCUTotal,SpeedMean,PCUMway,PCUDual,PCUSing,SpdMway,SpdDual,SpdSing,MSC1,MSC2,MSC3,MSC4,MSC5,MSC6,DSC1,DSC2,DSC3,DSC4,DSC5,DSC6,SSC1,SSC2,SSC3,SSC4,SSC5,SSC6,SSC7,SSC8,SpdMSC1,SpdMSC2,SpdMSC3,SpdMSC4,SpdMSC5,SpdMSC6,SpdDSC1,SpdDSC2,SpdDSC3,SpdDSC4,SpdDSC5,SpdDSC6,SpdSSC1,SpdSSC2,SpdSSC3,SpdSSC4,SpdSSC5,SpdSSC6,SpdSSC7,SpdSSC8,MWayLatent,DualLatent,SingLatent,MFullHrs,DFullHrs,SFullHrs,CostMway,CostDual,CostSing"
        ro.WriteLine(OutputRow)

        RoadElasticities = New IO.FileStream(DirPath & "Elasticity Files\TR" & Strategy & "\RoadLinkElasticities.csv", IO.FileMode.Open, IO.FileAccess.Read)
        re = New IO.StreamReader(RoadElasticities, System.Text.Encoding.Default)
        'read header row
        row = re.ReadLine

        'if the model is building capacity then create new capacity file
        If BuildInfra = True Then
            RoadLinkNewCap = New IO.FileStream(DirPath & FilePrefix & "RoadLinkNewCap.csv", IO.FileMode.Create, IO.FileAccess.Write)
            rlnc = New IO.StreamWriter(RoadLinkNewCap, System.Text.Encoding.Default)
            'write header row
            OutputRow = "FlowID,Yeary,RoadType,LanesAdded"
            rlnc.WriteLine(OutputRow)
        End If

        'if using variable trip rates then set up the trip rate variable
        If TripRates = "Strategy" Then
            StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
            rls = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
            'read header row
            rls.ReadLine()
            For r = 1 To 90
                row = rls.ReadLine
                stratarray = Split(row, ",")
                RdTripRates(0, r) = stratarray(91)
                RdTripRates(1, r) = stratarray(92)
            Next
            rls.Close()
        End If

    End Sub

    Sub DailyProfile()
        'sets up an array giving the proportion of trips made in each hour of the day

        Dim d As Byte

        d = 0

        Do While d < 24
            HourProportions(d) = TimeProfile(d)
            d += 1
        Loop

    End Sub

    Sub LoadInputRow()
        Dim varnum As Integer

        FlowDetails = Split(InputFlow, ",")
        FlowID = FlowDetails(0)
        Zone1 = FlowDetails(1)
        Zone2 = FlowDetails(2)
        RoadTypeLanes(0) = FlowDetails(4)
        RoadTypeLanes(1) = FlowDetails(5)
        RoadTypeLanes(2) = FlowDetails(6)
        'need the total lanes to start with as stable variable - this is because once there has been any capacity change will need to recalculate the base flows per lane each year
        TotalLanesOriginal = RoadTypeLanes(0) + RoadTypeLanes(1) + RoadTypeLanes(2)
        'set road type lanes new equal to road type lanes to start with, otherwise it will assume a capacity change to start with
        RoadTypeLanesNew(0) = FlowDetails(4)
        RoadTypeLanesNew(1) = FlowDetails(5)
        RoadTypeLanesNew(2) = FlowDetails(6)
        SpeedCatFlows(0) = FlowDetails(8)
        SpeedCatFlows(1) = FlowDetails(9)
        SpeedCatFlows(2) = FlowDetails(10)
        SpeedCatFlows(3) = FlowDetails(11)
        SpeedCatFlows(4) = FlowDetails(12)
        SpeedCatFlows(5) = FlowDetails(13)
        SpeedCatFlows(6) = FlowDetails(14)
        SpeedCatFlows(7) = FlowDetails(15)
        SpeedCatFlows(8) = FlowDetails(16)
        SpeedCatFlows(9) = FlowDetails(17)
        SpeedCatFlows(10) = FlowDetails(18)
        SpeedCatFlows(11) = FlowDetails(19)
        SpeedCatFlows(12) = FlowDetails(20)
        SpeedCatFlows(13) = FlowDetails(21)
        SpeedCatFlows(14) = FlowDetails(22)
        SpeedCatFlows(15) = FlowDetails(23)
        SpeedCatFlows(16) = FlowDetails(24)
        SpeedCatFlows(17) = FlowDetails(25)
        SpeedCatFlows(18) = FlowDetails(26)
        SpeedCatFlows(19) = FlowDetails(27)
        Z1Pop = FlowDetails(28)
        Z2Pop = FlowDetails(29)
        Z1GVA = FlowDetails(30)
        Z2GVA = FlowDetails(31)
        For c = 0 To 24
            CostOld(0, c) = FlowDetails(32)
        Next
        MaxCap(0) = FlowDetails(33)
        MaxCap(1) = FlowDetails(34)
        MaxCap(2) = FlowDetails(35)
        varnum = 36
        For x = 1 To 19
            For c = 0 To 24
                CostOld(x, c) = FlowDetails(varnum)
            Next
            varnum += 1
        Next
        'if using congestion charge then get fixed costs
        If CongestionCharge = True Then
            StandingCosts(0) = CostOld(0, 1) * 0.6936
            StandingCosts(1) = CostOld(1, 1) * ((0.7663 * 0.598) + (0.8089 * 0.402))
            StandingCosts(2) = CostOld(2, 1) * 0.845
            StandingCosts(3) = CostOld(3, 1) * 0.8699
            StandingCosts(4) = CostOld(4, 1) * 0.7791
            StandingCosts(5) = CostOld(5, 1) * 0.7065
            StandingCosts(6) = CostOld(6, 1) * 0.6936
            StandingCosts(7) = CostOld(7, 1) * ((0.7663 * 0.598) + (0.8089 * 0.402))
            StandingCosts(8) = CostOld(8, 1) * 0.845
            StandingCosts(9) = CostOld(9, 1) * 0.8699
            StandingCosts(10) = CostOld(10, 1) * 0.7791
            StandingCosts(11) = CostOld(11, 1) * 0.7065
            StandingCosts(12) = CostOld(12, 1) * 0.6936
            StandingCosts(13) = CostOld(13, 1) * ((0.7663 * 0.598) + (0.8089 * 0.402))
            StandingCosts(14) = CostOld(14, 1) * 0.845
            StandingCosts(15) = CostOld(15, 1) * 0.8699
            StandingCosts(16) = CostOld(16, 1) * 0.7791
            StandingCosts(17) = CostOld(17, 1) * 0.7791
            StandingCosts(18) = CostOld(18, 1) * 0.7065
            StandingCosts(19) = CostOld(19, 1) * 0.7065
        End If
        AddedLanes(0) = 0
        AddedLanes(1) = 0
        AddedLanes(2) = 0

        '15/08 modification clear the congestion charge old variable
        ReDim ChargeNew(19, 24)
        ReDim ChargeOld(19, 24)

    End Sub

    Sub ReadRoadEls()
        Dim row As String
        Dim elstring() As String
        Dim yearcheck As Integer
        Dim elcount As Integer

        yearcheck = 1

        Do
            'read in line from elasticities file
            row = re.ReadLine
            If row Is Nothing Then
                Exit Do
            End If
            'split it into array - 1 is passpop, 2 is passgva, 3 is passspd, 4 is passcost, 5 is frtpop, 6 is frtgva, 7 is frtspd, 8 is frtcost, 9 is spdcapu
            elstring = Split(row, ",")
            elcount = 1
            Do While elcount < 10
                RoadEls(elcount, yearcheck) = elstring(elcount)
                elcount += 1
            Loop
            yearcheck += 1
        Loop

    End Sub

    Sub NewSpeedCatFlows()
        'if capacity has changed in the last year
        If CapChangeNew = True Then

            sc = 0

            Do While sc < 20
                Call AssignRoadType()
                If RoadTypeLanesNew(RoadType) > 0 Then
                    If SpeedCatFlows(sc) > 0 Then
                        SpeedCatFlows(sc) = (SpeedCatFlows(sc) * RoadTypeLanes(RoadType)) / RoadTypeLanesNew(RoadType)
                    End If
                    'v1.4 modification - need to scale these figures too
                    For hr = 0 To 23
                        OldHourlyFlows(sc, hr) = (OldHourlyFlows(sc, hr) * RoadTypeLanes(RoadType)) / RoadTypeLanesNew(RoadType)
                        RoadTypeFlows(RoadType, hr) = (RoadTypeFlows(RoadType, hr) * RoadTypeLanes(RoadType)) / RoadTypeLanesNew(RoadType)
                    Next
                    '***v1.4 modification finishes here
                End If
                'v1.2 modification - add latent demand to the previous demand, and set latent demand variables to zero
                'v1.2 additional modification - also update the road type flows variables
                h = 0
                Do Until h > 23
                    OldHourlyFlows(sc, h) += LatentHourlyFlows(sc, h)
                    RoadTypeFlows(RoadType, h) += LatentHourlyFlows(sc, h)
                    LatentHourlyFlows(sc, h) = 0
                    h += 1
                Loop
                sc += 1
            Loop

        Else
            'if not then don't need to do anything
        End If

        'update lane variables to current values
        RoadTypeLanes(0) = RoadTypeLanesNew(0)
        RoadTypeLanes(1) = RoadTypeLanesNew(1)
        RoadTypeLanes(2) = RoadTypeLanesNew(2)
    End Sub

    Sub StartFlows()
        Dim t As Integer

        'divide the total flows for each flow type by the proportions given by the daily travel profile
        'store these proportional flows in a two dimensional array
        'sum the hourly values to give a total value for each road type
        'note that in this and subsequent subs the 'sc' and 'h' values are 1 lower than might 'intuitively' be expected, because first element in arrays is numbered 0

        ReDim RoadTypeFlows(2, 23)

        sc = 0
        h = 0

        Do While sc < 20
            Call AssignRoadType()
            Do While h < 24
                OldHourlyFlows(sc, h) = SpeedCatFlows(sc) * HourProportions(h)
                RoadTypeFlows(RoadType, h) = RoadTypeFlows(RoadType, h) + OldHourlyFlows(sc, h)
                h += 1
            Loop
            sc += 1
            h = 0
        Loop

        'v1.2 completed modification check if the hourly flow in the busiest hour (0800-0900, so h=8) exceeds the maximum capacity for the road type, and if it does then update that maximum capacity
        'the maximum capacity is rounded up to the nearest whole number to overcome problems caused by variables storing a different number of decimal places
        For t = 0 To 2
            If RoadTypeFlows(t, 8) > MaxCap(t) Then
                MaxCap(t) = Math.Round(RoadTypeFlows(t, 8) + 0.5)
            End If
        Next
    End Sub
    Sub GetExternalValues()
        'v1.2 modification - now get external variable values for maximum road capacities

        Dim rownum As Long
        Dim row As String
        Dim ExtVarRow() As String
        Dim r As Byte
        Dim YearCount As Integer

        rownum = 1
        YearCount = 1
        Do While rownum < 91
            'loop through 90 rows in the external variables file, storing the values in the external variable values array
            row = ev.ReadLine
            ExtVarRow = Split(row, ",")
            If ExtVarRow(1) = YearCount Then
                'as long as the year counter corresponds to the year value in the input data, write values to the array
                For r = 1 To 31
                    ExternalValues(r, rownum) = ExtVarRow(r)
                Next
                rownum += 1
            Else
                '****otherwise stop the model and write an error to the log file
            End If
            YearCount += 1
        Loop

    End Sub

    Sub BaseSpeeds()
        'calculate the speed for each of the hourly segments for each of the speed categories
        'in this case don't need to iterate to get speeds - we know the total base flow for each road type, so just use the speed calculator to adjust the speeds if conditions are congested - flows are observed and therefore held constant
        'if this is the first year, we need to calculate the base speeds from the input data

        If YearNum = 1 Then
            sc = 0
            h = 0

            'v1.2 mod update the maximum capacity values
            'v1.3 mod moved inside the if clause as otherwise fails in 1st year if maximum capacity has been reset in base year
            If ExternalValues(10, YearNum) > MaxCap(0) Then
                MaxCap(0) = ExternalValues(10, YearNum)
            End If
            If ExternalValues(11, YearNum) > MaxCap(1) Then
                MaxCap(1) = ExternalValues(11, YearNum)
            End If
            If ExternalValues(12, YearNum) > MaxCap(2) Then
                MaxCap(2) = ExternalValues(12, YearNum)
            End If

            Do While sc < 20
                Do While h < 24
                    Call AssignRoadType()
                    'if traffic less than free flow capacity then adopt free flow speed
                    If RoadTypeFlows(RoadType, h) < (FreeFlowCU * MaxCap(RoadType)) Then
                        HourlySpeeds(sc, h) = FreeFlowSpeeds(sc)
                    ElseIf RoadTypeFlows(RoadType, h) <= MaxCap(RoadType) Then
                        'otherwise if it is in between the free flow capacity and the maximum capacity then use the speed calculator
                        'because this is the first year set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(RoadType)
                        FlowNew = RoadTypeFlows(RoadType, h)
                        SpeedOld = FreeFlowSpeeds(sc)
                        Call SpeedCalc()
                        HourlySpeeds(sc, h) = SpeedNew
                    Else
                        'this shouldn't happen in the base year, as we should already have reset the maximum capacity variable in the start flows sub, so write error to log file and exit model
                        LogLine = "ERROR in interzonal road model - maximum capacity exceeded in base year for Flow " & FlowID & ", road type " & RoadType & ", hour " & h & ". Model run terminated."
                        lf.WriteLine(LogLine)
                        lf.Close()
                        ro.Close()
                        End
                    End If
                    h += 1
                Loop
                sc += 1
                h = 0
            Loop
        Else
            'v1.3 mod moved inside the if clause as otherwise fails in 1st year if maximum capacity has been reset in base year
            MaxCap(0) = ExternalValues(10, YearNum)
            MaxCap(1) = ExternalValues(11, YearNum)
            MaxCap(2) = ExternalValues(12, YearNum)
            'if it isn't the first year then we need to check if the capacity has changed
            If CapChangeNew = True Then
                'if capacity has changed since the previous year then need to recalculate the base speeds based on the new capacities (having already recalculated trips per lane in previous sub)
                h = 0
                'v1.3 MODIFICATION - need to split this out so that we do each road type separately
                Do While h < 24
                    'for class 0
                    If RoadTypeFlows(0, h) < (FreeFlowCU * MaxCap(0)) Then
                        For t = 0 To 5
                            'if traffic less than free flow capacity then adopt free flow speed
                            HourlySpeeds(t, h) = FreeFlowSpeeds(t)
                        Next
                    ElseIf RoadTypeFlows(0, h) <= MaxCap(0) Then
                        'otherwise if it is in between the free flow capacity and the maximum capacity then use the speed calculator
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(0)
                        FlowNew = RoadTypeFlows(0, h)
                        For t = 0 To 5
                            SpeedOld = FreeFlowSpeeds(t)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(t, h) = SpeedNew
                        Next
                    Else
                        'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable
                        LatentTraffic = RoadTypeFlows(0, h) - MaxCap(0)
                        For t = 0 To 5
                            LatentHourlyFlows(t, h) += (OldHourlyFlows(t, h) / RoadTypeFlows(0, h)) * LatentTraffic
                        Next
                        'set the traffic level as equal to the road capacity
                        RoadTypeFlows(0, h) = MaxCap(0)
                        'then calculate the speed as before
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(0)
                        FlowNew = RoadTypeFlows(0, h)
                        For t = 0 To 5
                            SpeedOld = FreeFlowSpeeds(t)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(t, h) = SpeedNew
                        Next
                    End If
                    'for class 1
                    If RoadTypeFlows(1, h) < (FreeFlowCU * MaxCap(1)) Then
                        For t = 6 To 11
                            'if traffic less than free flow capacity then adopt free flow speed
                            HourlySpeeds(t, h) = FreeFlowSpeeds(t)
                        Next
                    ElseIf RoadTypeFlows(1, h) <= MaxCap(1) Then
                        'otherwise if it is in between the free flow capacity and the maximum capacity then use the speed calculator
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(1)
                        FlowNew = RoadTypeFlows(1, h)
                        For t = 6 To 11
                            SpeedOld = FreeFlowSpeeds(t)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(t, h) = SpeedNew
                        Next
                    Else
                        'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable
                        LatentTraffic = RoadTypeFlows(1, h) - MaxCap(1)
                        For t = 6 To 11
                            LatentHourlyFlows(t, h) += (OldHourlyFlows(t, h) / RoadTypeFlows(1, h)) * LatentTraffic
                        Next
                        'set the traffic level as equal to the road capacity
                        RoadTypeFlows(1, h) = MaxCap(1)
                        'then calculate the speed as before
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(1)
                        FlowNew = RoadTypeFlows(1, h)
                        For t = 6 To 11
                            SpeedOld = FreeFlowSpeeds(t)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(t, h) = SpeedNew
                        Next
                    End If
                    'for class 2
                    If RoadTypeFlows(2, h) < (FreeFlowCU * MaxCap(2)) Then
                        For t = 12 To 19
                            'if traffic less than free flow capacity then adopt free flow speed
                            HourlySpeeds(t, h) = FreeFlowSpeeds(t)
                        Next
                    ElseIf RoadTypeFlows(2, h) <= MaxCap(2) Then
                        'otherwise if it is in between the free flow capacity and the maximum capacity then use the speed calculator
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(2)
                        FlowNew = RoadTypeFlows(2, h)
                        For t = 12 To 19
                            SpeedOld = FreeFlowSpeeds(t)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(t, h) = SpeedNew
                        Next
                    Else
                        'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable
                        LatentTraffic = RoadTypeFlows(2, h) - MaxCap(2)
                        For t = 12 To 19
                            LatentHourlyFlows(t, h) += (OldHourlyFlows(t, h) / RoadTypeFlows(2, h)) * LatentTraffic
                        Next
                        'set the traffic level as equal to the road capacity
                        RoadTypeFlows(2, h) = MaxCap(2)
                        'then calculate the speed as before
                        'because capacity has changed set the old speed as the free flow speed
                        FlowOld = FreeFlowCU * MaxCap(2)
                        FlowNew = RoadTypeFlows(2, h)
                        For t = 12 To 19
                            SpeedOld = FreeFlowSpeeds(t)
                            sc = t
                            Call SpeedCalc()
                            HourlySpeeds(t, h) = SpeedNew
                        Next
                    End If
                    h += 1
                Loop

            Else
                'if capacity hasn't changed then can simply take the final set of speeds from the last set of output data
                sc = 0
                h = 0

                Do While sc < 20
                    Do While h < 24
                        HourlySpeeds(sc, h) = NewHourlySpeeds(sc, h)
                        h += 1
                    Loop
                    sc += 1
                    h = 0
                Loop
            End If

        End If
    End Sub
    Sub AssignRoadType()
        'assigns a speed category to a road type
        Select Case sc
            Case 0, 1, 2, 3, 4, 5
                RoadType = 0

            Case 6, 7, 8, 9, 10, 11
                RoadType = 1

            Case 12, 13, 14, 15, 16, 17, 18, 19
                RoadType = 2

        End Select
    End Sub

    Sub SpeedCalc()
        'calculates change in speed as result of change in flow
        If VariableEl = True Then
            OldX = SpeedOld
            OldY = FlowOld
            NewY = FlowNew
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(9, YearNum)
                Call VarElCalc()
                SpeedRatio = VarRat
            Else
                SpeedRatio = (FlowNew / FlowOld) ^ RoadEls(9, YearNum)
            End If
        Else
            SpeedRatio = (FlowNew / FlowOld) ^ RoadEls(9, YearNum)
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
        For c = 0 To 24
            CostNew(0, c) = ExternalValues(6, YearNum)
        Next


        ratnum = 13
        For x = 1 To 19
            For c = 0 To 24
                CostNew(x, c) = ExternalValues(ratnum, YearNum)
            Next
            ratnum += 1
        Next

        sc = 0
        h = 0

        'calculate the individual variable ratios for passenger traffic
        'v1.3 mod
        If TripRates = "Strategy" Then
            rat1 = (((ExternalValues(2, YearNum) + ExternalValues(3, YearNum)) * RdTripRates(0, YearNum)) / (Z1Pop + Z2Pop)) ^ RoadEls(1, YearNum)
        Else
            rat1 = ((ExternalValues(2, YearNum) + ExternalValues(3, YearNum)) / (Z1Pop + Z2Pop)) ^ RoadEls(1, YearNum)
        End If

        rat3 = ((ExternalValues(4, YearNum) + ExternalValues(5, YearNum)) / (Z1GVA + Z2GVA)) ^ RoadEls(2, YearNum)
        'initially set speed new and speed old as equal to speed in previous year, so ratio = 1 - can be altered if desired as part of scenario
        rat5 = 1 ^ RoadEls(3, YearNum)

        'cost ratio now estimated in getflowratio sub

        'calculate the individual variable ratios for freight traffic
        'v1.3 mod
        If TripRates = "Strategy" Then
            ratf1 = (((ExternalValues(2, YearNum) + ExternalValues(3, YearNum)) * RdTripRates(1, YearNum)) / (Z1Pop + Z2Pop)) ^ RoadEls(5, YearNum)
        Else
            ratf1 = ((ExternalValues(2, YearNum) + ExternalValues(3, YearNum)) / (Z1Pop + Z2Pop)) ^ RoadEls(5, YearNum)
        End If

        ratf3 = ((ExternalValues(4, YearNum) + ExternalValues(5, YearNum)) / (Z1GVA + Z2GVA)) ^ RoadEls(6, YearNum)
        '***NOTE - if altering this elasticity will also need to alter the flow-speed iteration process as this used the rat5 variable only
        ratf5 = 1 ^ RoadEls(7, YearNum)
        'cost ratio now estimated in getflowratio sub

        Do While h < 24
            '***arguably we should iterate across all categories simultaneously, but this will complicate the iteration
            'do iteration for categories within road class 0
            
            RoadType = 0
            sc = 0
            ClassFlow = OldHourlyFlows(0, h) + OldHourlyFlows(1, h) + OldHourlyFlows(2, h) + OldHourlyFlows(3, h) + OldHourlyFlows(4, h) + OldHourlyFlows(5, h)
            If ClassFlow > 0 Then
                'scale hourly flows based on all ratios except that for speed, which requires iteration
                'sum as we go along to get an initial figure for the new total road class flow
                ClassFlowNew = 0
                Do While sc < 6
                    Call GetFlowRatio()
                    ScaledHourlyFlows(sc, h) = OldHourlyFlows(sc, h) * FlowRatio
                    ClassFlowNew += ScaledHourlyFlows(sc, h)
                    sc += 1
                Loop
                'we can assume that change in all speeds within class is proportionally the same, so only need to calculate single speed ratio
                sc = 0
                'v1.2 completed modification
                'set old speed to the initial hourly speed for first speed category to start with
                SpeedOld = HourlySpeeds(sc, h)
                'set speed original to equal speedold to save the original speed, which is used for calculating the hourly speeds
                SpeedOriginal = SpeedOld
                If ClassFlowNew < (FreeFlowCU * MaxCap(RoadType)) Then
                    'if road class flow is less than the free flow capacity then just use the free flow speed
                    FlowNew = ClassFlowNew
                    SpeedNew = SpeedOld
                ElseIf ClassFlowNew <= MaxCap(RoadType) Then
                    'in this case we do still call the flow-speed iterator
                    Call FlowSpeedIterate()
                Else
                    'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable 
                    LatentTraffic = ClassFlowNew - MaxCap(RoadType)
                    sc = 0
                    Do While sc < 6
                        LatentHourlyFlows(sc, h) += (OldHourlyFlows(sc, h) / ClassFlow) * LatentTraffic
                        sc += 1
                    Loop
                    'set the traffic level as equal to the road capacity
                    RoadTypeFlows(RoadType, h) = MaxCap(RoadType)
                    'then calculate the speed as before
                    'because capacity has changed set the old speed as the free flow speed
                    FlowOld = ClassFlow
                    FlowNew = RoadTypeFlows(RoadType, h)
                    Call SpeedCalc()
                End If
                'end of modification
                'set value in new hourly flows array - to equal stable flow value - and in new hourly speeds array
                'this updates the initial scaled hourly flow value by multiplying it by the ratio resulting from the iteration
                sc = 0
                Do While sc < 6
                    NewHourlyFlows(sc, h) = ScaledHourlyFlows(sc, h) * (FlowNew / ClassFlowNew)
                    NewHourlySpeeds(sc, h) = HourlySpeeds(sc, h) * (SpeedNew / SpeedOriginal)
                    sc += 1
                Loop
            Else
                Do While sc < 6
                    NewHourlyFlows(sc, h) = 0
                    NewHourlySpeeds(sc, h) = HourlySpeeds(sc, h)
                    sc += 1
                Loop

            End If
            'do iteration for categories within road class 1
            RoadType = 1
            sc = 6
            ClassFlow = OldHourlyFlows(6, h) + OldHourlyFlows(7, h) + OldHourlyFlows(8, h) + OldHourlyFlows(9, h) + OldHourlyFlows(10, h) + OldHourlyFlows(11, h)
            If ClassFlow > 0 Then
                'scale hourly flows based on all ratios except that for speed, which requires iteration
                'sum as we go along to get an initial figure for the new total road class flow
                ClassFlowNew = 0
                Do While sc < 12
                    Call GetFlowRatio()
                    ScaledHourlyFlows(sc, h) = OldHourlyFlows(sc, h) * FlowRatio
                    ClassFlowNew += ScaledHourlyFlows(sc, h)
                    sc += 1
                Loop
                'we can assume that change in all speeds within class is proportionally the same, so only need to calculate single speed ratio
                sc = 6
                'v1.2 completed modification
                'set old speed to the initial hourly speed for first speed category to start with
                SpeedOld = HourlySpeeds(sc, h)
                'set speed original to equal speedold to save the original speed, which is used for calculating the hourly speeds
                SpeedOriginal = SpeedOld
                If ClassFlowNew < (FreeFlowCU * MaxCap(RoadType)) Then
                    'if road class flow is less than the free flow capacity then just use the free flow speed
                    FlowNew = ClassFlowNew
                    SpeedNew = SpeedOld
                ElseIf ClassFlowNew <= MaxCap(RoadType) Then
                    'in this case we do still call the flow-speed iterator
                    Call FlowSpeedIterate()
                Else
                    'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable 
                    LatentTraffic = ClassFlowNew - MaxCap(RoadType)
                    sc = 6
                    Do While sc < 12
                        LatentHourlyFlows(sc, h) += (OldHourlyFlows(sc, h) / ClassFlow) * LatentTraffic
                        sc += 1
                    Loop
                    'set the traffic level as equal to the road capacity
                    RoadTypeFlows(RoadType, h) = MaxCap(RoadType)
                    'then calculate the speed as before
                    'because capacity has changed set the old speed as the free flow speed
                    FlowOld = ClassFlow
                    FlowNew = RoadTypeFlows(RoadType, h)
                    Call SpeedCalc()
                End If
                'end of modification
                'set value in new hourly flows array - to equal stable flow value - and in new hourly speeds array
                'this updates the initial scaled hourly flow value by multiplying it by the ratio resulting from the iteration
                sc = 6
                Do While sc < 12
                    NewHourlyFlows(sc, h) = ScaledHourlyFlows(sc, h) * (FlowNew / ClassFlowNew)
                    NewHourlySpeeds(sc, h) = HourlySpeeds(sc, h) * (SpeedNew / SpeedOriginal)
                    sc += 1
                Loop
            Else
                Do While sc < 12
                    NewHourlyFlows(sc, h) = 0
                    NewHourlySpeeds(sc, h) = HourlySpeeds(sc, h)
                    sc += 1
                Loop
            End If
            'do iteration for categories within road class 2
            RoadType = 2
            sc = 12
            ClassFlow = OldHourlyFlows(12, h) + OldHourlyFlows(13, h) + OldHourlyFlows(14, h) + OldHourlyFlows(15, h) + OldHourlyFlows(16, h) + OldHourlyFlows(17, h) + OldHourlyFlows(18, h) + OldHourlyFlows(19, h)
            If ClassFlow > 0 Then
                'scale hourly flows based on all ratios except that for speed, which requires iteration
                'sum as we go along to get an initial figure for the new total road class flow
                ClassFlowNew = 0
                Do While sc < 20
                    Call GetFlowRatio()
                    ScaledHourlyFlows(sc, h) = OldHourlyFlows(sc, h) * FlowRatio
                    ClassFlowNew += ScaledHourlyFlows(sc, h)
                    sc += 1
                Loop
                'we can assume that change in all speeds within class is proportionally the same, so only need to calculate single ratio
                sc = 12
                'v1.2 completed modification
                'set old speed to the initial hourly speed for first speed category to start with
                SpeedOld = HourlySpeeds(sc, h)
                'set speed original to equal speedold to save the original speed, which is used for calculating the hourly speeds
                SpeedOriginal = SpeedOld
                If ClassFlowNew < (FreeFlowCU * MaxCap(RoadType)) Then
                    'if road class flow is less than the free flow capacity then just use the free flow speed
                    FlowNew = ClassFlowNew
                    SpeedNew = SpeedOld
                ElseIf ClassFlowNew <= MaxCap(RoadType) Then
                    'in this case we do still call the flow-speed iterator
                    Call FlowSpeedIterate()
                Else
                    'otherwise demand has exceeded capacity so we need to move some of the traffic to the latent variable 
                    LatentTraffic = ClassFlowNew - MaxCap(RoadType)
                    sc = 12
                    Do While sc < 20
                        LatentHourlyFlows(sc, h) += (OldHourlyFlows(sc, h) / ClassFlow) * LatentTraffic
                        sc += 1
                    Loop
                    'set the traffic level as equal to the road capacity
                    RoadTypeFlows(RoadType, h) = MaxCap(RoadType)
                    'then calculate the speed as before
                    'because capacity has changed set the old speed as the free flow speed
                    FlowOld = ClassFlow
                    FlowNew = RoadTypeFlows(RoadType, h)
                    Call SpeedCalc()
                End If
                'end of modification
                'set value in new hourly flows array - to equal stable flow value - and in new hourly speeds array
                'this updates the initial scaled hourly flow value by multiplying it by the ratio resulting from the iteration
                sc = 12
                Do While sc < 20
                    NewHourlyFlows(sc, h) = ScaledHourlyFlows(sc, h) * (FlowNew / ClassFlowNew)
                    NewHourlySpeeds(sc, h) = HourlySpeeds(sc, h) * (SpeedNew / SpeedOriginal)
                    sc += 1
                Loop
            Else
                Do While sc < 20
                    NewHourlyFlows(sc, h) = 0
                    NewHourlySpeeds(sc, h) = HourlySpeeds(sc, h)
                    sc += 1
                Loop
            End If
            'if using smart logistics then need to scale the HGV traffic
            If SmartFrt = True Then
                If YearNum > SmFrtIntro Then
                    'if year is after the introduction year and before or equal to the year when the final effect is realised then need to scale HGV traffic
                    If YearNum <= SmFrtIntro + SmFrtYears Then
                        NewHourlyFlows(4, h) = NewHourlyFlows(4, h) * (1 - (SmFrtPer / SmFrtYears))
                        NewHourlyFlows(5, h) = NewHourlyFlows(5, h) * (1 - (SmFrtPer / SmFrtYears))
                        NewHourlyFlows(10, h) = NewHourlyFlows(10, h) * (1 - (SmFrtPer / SmFrtYears))
                        NewHourlyFlows(11, h) = NewHourlyFlows(11, h) * (1 - (SmFrtPer / SmFrtYears))
                        For x = 16 To 19
                            NewHourlyFlows(x, h) = NewHourlyFlows(x, h) * (1 - (SmFrtPer / SmFrtYears))
                        Next
                        'also need to scale the equivalent latent traffic by the same amount
                        LatentHourlyFlows(4, h) = LatentHourlyFlows(4, h) * (1 - (SmFrtPer / SmFrtYears))
                        LatentHourlyFlows(5, h) = LatentHourlyFlows(5, h) * (1 - (SmFrtPer / SmFrtYears))
                        LatentHourlyFlows(10, h) = LatentHourlyFlows(10, h) * (1 - (SmFrtPer / SmFrtYears))
                        LatentHourlyFlows(11, h) = LatentHourlyFlows(11, h) * (1 - (SmFrtPer / SmFrtYears))
                        For x = 16 To 19
                            LatentHourlyFlows(x, h) = LatentHourlyFlows(x, h) * (1 - (SmFrtPer / SmFrtYears))
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
            If YearNum >= ConChargeYear Then
                If ClassFlowNew < (FreeFlowCU * MaxCap(RoadType)) Then
                    conchargeprop = 0
                Else
                    conchargeprop = ClassFlowNew / MaxCap(RoadType)
                    If conchargeprop > 0.9 Then
                        conchargeprop = conchargeprop
                    End If
                End If
                'use select case to set charges, as no charge set for buses
                Select Case sc
                    Case 0, 1, 2, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 16, 17, 18, 19
                        ChargeNew(sc, h) = StandingCosts(sc) * ConChargePer * (conchargeprop ^ 2)
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
            OldX = OldHourlyFlows(sc, h)
            'pop ratio
            OldY = Z1Pop + Z2Pop
            If TripRates = "Strategy" Then
                NewY = (ExternalValues(2, YearNum) + ExternalValues(3, YearNum)) * RdTripRates(1, YearNum)
            Else
                NewY = ExternalValues(2, YearNum) + ExternalValues(3, YearNum)
            End If
            NewY = ExternalValues(2, YearNum) + ExternalValues(3, YearNum)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(1, YearNum)
                Call VarElCalc()
                rat1 = VarRat
            End If
            'gva ratio
            OldY = Z1GVA + Z2GVA
            NewY = ExternalValues(4, YearNum) + ExternalValues(5, YearNum)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(2, YearNum)
                Call VarElCalc()
                rat3 = VarRat
            End If
            'don't need to include speed ratio as this is held at 1 initially
            'cost ratios
            'modification - don't need to calculate them all each time, only the one for the speed category we are currently looking at
            OldY = CostOld(sc, h) + ChargeOld(sc, h)
            NewY = CostNew(sc, h) + ChargeNew(sc, h)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(4, YearNum)
                Call VarElCalc()
                rat6(sc) = VarRat
            Else
                rat6(sc) = (NewY / OldY) ^ RoadEls(4, YearNum)
            End If
            'freight pop ratio
            OldY = Z1Pop + Z2Pop
            If TripRates = "Strategy" Then
                NewY = (ExternalValues(2, YearNum) + ExternalValues(3, YearNum)) * RdTripRates(1, YearNum)
            Else
                NewY = ExternalValues(2, YearNum) + ExternalValues(3, YearNum)
            End If
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(5, YearNum)
                Call VarElCalc()
                ratf1 = VarRat
            End If
            'freight gva ratio
            OldY = Z1GVA + Z2GVA
            NewY = ExternalValues(4, YearNum) + ExternalValues(5, YearNum)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(6, YearNum)
                Call VarElCalc()
                ratf3 = VarRat
            End If
            'don't need to include speed ratio as this is held at 1 initially
            'freight cost ratios
            OldY = CostOld(sc, h) + ChargeOld(sc, h)
            NewY = CostNew(sc, h) + ChargeNew(sc, h)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RoadEls(8, YearNum)
                Call VarElCalc()
                ratf6(sc) = VarRat
            Else
                ratf6(sc) = (CostNew(sc, h) / CostOld(sc, h)) ^ RoadEls(8, YearNum)
            End If
        Else
            'still need to recalculate ratio if using congestion charging and to calculate cost ratio if not (latter now moved from hourly flow calc sub
            If CongestionCharge = True Then
                OldY = CostOld(sc, h) + ChargeOld(sc, h)
                NewY = CostNew(sc, h) + ChargeNew(sc, h)
                rat6(sc) = (NewY / OldY) ^ RoadEls(4, YearNum)
                OldY = CostOld(sc, h) + ChargeOld(sc, h)
                NewY = CostNew(sc, h) + ChargeNew(sc, h)
                ratf6(sc) = (NewY / OldY) ^ RoadEls(8, YearNum)
            Else
                rat6(sc) = (CostNew(sc, h) / CostOld(sc, h)) ^ RoadEls(4, YearNum)
                ratf6(sc) = (CostNew(sc, h) / CostOld(sc, h)) ^ RoadEls(8, YearNum)
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
                LogLine = "ERROR in Road Link Module: Flow" & FlowID & " Year" & YearNum & " Road Type " & RoadType & " speed and flow failed to converge after 1000 iterations"
                lf.WriteLine(LogLine)
                lf.Close()
                ro.Close()
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
                SpeedCatSpeedsNew(sc) = SpeedCatSpeedsNew(sc) + (NewHourlyFlows(sc, h) * NewHourlySpeeds(sc, h))
                LatentFlows(sc) = LatentFlows(sc) + LatentHourlyFlows(sc, h)
                h += 1
            Loop
            'only calculate speed if flow is greater than zero, otherwise set it to the last new hourly speed (as all will be free flow)
            If SpeedCatFlowsNew(sc) > 0 Then
                SpeedCatSpeedsNew(sc) = SpeedCatSpeedsNew(sc) / SpeedCatFlowsNew(sc)
                MeanSpeedNew = MeanSpeedNew + (SpeedCatFlowsNew(sc) * SpeedCatSpeedsNew(sc))
            Else
                SpeedCatSpeedsNew(sc) = NewHourlySpeeds(sc, h)
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
        MwayFlowNew = MwayFlowNew * RoadTypeLanes(0)
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
        DualFlowNew = DualFlowNew * RoadTypeLanes(1)
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
        SingFlowNew = SingFlowNew * RoadTypeLanes(2)
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
                latenttrips += LatentHourlyFlows(sc, h)
                sc += 1
            Loop
            If latenttrips > 0 Then
                MFullHrs += 1
            End If
            latenttrips = 0
            Do Until sc = 12
                latenttrips += LatentHourlyFlows(sc, h)
                sc += 1
            Loop
            If latenttrips > 0 Then
                DFullHrs += 1
            End If
            latenttrips = 0
            Do Until sc = 20
                latenttrips += LatentHourlyFlows(sc, h)
                sc += 1
            Loop
            If latenttrips > 0 Then
                SFullHrs += 1
            End If
            h += 1
        Loop
    End Sub
    Sub WriteOutputRow()
        'v1.2 modification completed - now writes latent demand and number of full hours on each road type
        OutputRow = FlowID & "," & YearNum & "," & TotalFlowNew & "," & MeanSpeedNew & "," & MwayFlowNew & "," & DualFlowNew & "," & SingFlowNew & "," & MWaySpdNew & "," & DualSpdNew & "," & SingSpdNew & ","
        sc = 0
        Do While sc < 20
            OutputRow = OutputRow & SpeedCatFlowsNew(sc) & ","
            sc += 1
        Loop
        sc = 0
        Do While sc < 20
            OutputRow = OutputRow & SpeedCatSpeedsNew(sc) & ","
            sc += 1
        Loop
        'v1.2 modification now also writes latent demand and number of full hours on each road type
        OutputRow = OutputRow & MWayLatFlowNew & "," & DualLatFlowNew & "," & SingLatFlowNew & "," & MFullHrs & "," & DFullHrs & "," & SFullHrs
        'v1.4 now also writes average cost for each flow type
        OutputRow = OutputRow & "," & MeanCostNew(0) & "," & MeanCostNew(1) & "," & MeanCostNew(2)
        ro.WriteLine(OutputRow)
    End Sub
    Sub WriteInputFile()
        'store outputs in temp file "Flows.csv"
        'StandardCost 0 - 19 use normal WriteLine function
        'hour 0 to 23 are stored in array and then WriteLine into the temp file to make it looks better

        Dim i As Integer
        Dim j As Integer
        Dim out As String(,)
        ReDim out(24, 20)
        Dim FlowFile As IO.FileStream
        Dim ff As IO.StreamWriter

        'create a temp file "Flows.csv"
        FlowFile = New IO.FileStream(DirPath & FilePrefix & "Flows.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        ff = New IO.StreamWriter(FlowFile, System.Text.Encoding.Default)

        'write header row
        OutputRow = "FlowID,Yeary,sc0,c1,sc2,sc3,sc4,sc5,sc6,sc7,sc8,sc9,sc10,c11,sc12,sc13,sc14,sc15,sc16,sc17,sc18,sc19"
        ff.WriteLine(OutputRow)

        'write second row
        OutputRow = FlowID & "," & YearNum & ","
        sc = 0
        Do While sc < 20
            OutputRow = OutputRow & SpeedCatFlowsNew(sc) & ","
            h = 0
            Do While h < 24
                out(h, sc) = NewHourlyFlows(sc, h)
                h += 1
            Loop
            sc += 1
        Loop
        ff.WriteLine(OutputRow)

        'write hour data 0 - 23 
        For i = 0 To 23
            OutputRow = "," & "hour" & i & ","
            For j = 0 To 20
                OutputRow = OutputRow & out(i, j) & ","
            Next j
            ff.WriteLine(OutputRow)
        Next i
        ff.Close()
    End Sub
    Sub ReadInputFile()
        Dim InputDetail As Double(,)
        ReDim InputDetail(25, 20)
        Dim i As Integer
        Dim j As Integer
        Dim result() As String
        Dim Input As String
        Dim FlowFile As IO.FileStream
        Dim ff As IO.StreamReader

        'read the temp file "Flows.csv"
        FlowFile = New IO.FileStream(DirPath & FilePrefix & "Flows.csv", IO.FileMode.Open, IO.FileAccess.Read)
        ff = New IO.StreamReader(FlowFile, System.Text.Encoding.Default)

        'read header line
        ff.ReadLine()

        'read standard cost value and each hour value into array "InputDetail"
        For i = 0 To 24
            Input = ff.ReadLine
            result = Split(Input, ",")
            For j = 0 To 19
                InputDetail(i, j) = CDbl(Val(result(j + 2)))
            Next
        Next

        'store the values into computer memory
        sc = 0
        Do While sc < 20
            SpeedCatFlowsNew(sc) = InputDetail(0, sc)
            h = 0
            Do While h < 24
                NewHourlyFlows(sc, h) = InputDetail(h + 1, sc)
                h += 1
            Loop
            sc += 1
        Loop
        ff.Close()

        'delete the temp file
        System.IO.File.Delete(DirPath & FilePrefix & "Flows.csv")

    End Sub

    Sub UpdateInputVars()
        'updates starting flows from end flows from previous iteration
        'v1.2 completed modification - now also updates old hourly flows with new hourly flow values from previous year, as no longer doing a proportional split of the total demand based on the time profile
        '...at the start of each year because this would keep getting rid of hours where the road was full

        Dim cu As Double
        Dim newcapstring As String

        ReDim RoadTypeFlows(2, 23)

        sc = 0
        Do While sc < 20
            SpeedCatFlows(sc) = SpeedCatFlowsNew(sc)
            'v1.2 modification - now also updates hourly flows
            'v1.2 additional modification - updates road type flows too
            Call AssignRoadType()
            h = 0
            Do Until h > 23
                OldHourlyFlows(sc, h) = NewHourlyFlows(sc, h)
                RoadTypeFlows(RoadType, h) = RoadTypeFlows(RoadType, h) + OldHourlyFlows(sc, h)
                'now updates charge variable too
                If CongestionCharge = True Then
                    ChargeOld(sc, h) = ChargeNew(sc, h)
                End If
                h += 1
            Loop
            sc += 1
        Loop

        'update population, gva and cost variables based on values from external variables input file
        Z1Pop = ExternalValues(2, YearNum)
        Z2Pop = ExternalValues(3, YearNum)
        Z1GVA = ExternalValues(4, YearNum)
        Z2GVA = ExternalValues(5, YearNum)
        For x = 0 To 19
            For c = 0 To 24
                CostOld(x, c) = CostNew(x, c)
            Next
        Next
        RoadTypeLanesNew(0) = ExternalValues(7, YearNum) + AddedLanes(0)
        RoadTypeLanesNew(1) = ExternalValues(8, YearNum) + AddedLanes(1)
        RoadTypeLanesNew(2) = ExternalValues(9, YearNum) + AddedLanes(2)

        'v1.4 blank mean cost variables
        MeanCostNew(0) = 0
        MeanCostNew(1) = 0
        MeanCostNew(2) = 0

        'if building capacity then check if new capacity is needed
        If BuildInfra = True Then
            'check motorways
            If RoadTypeLanesNew(0) > 0 Then
                h = 0
                Do While h < 24
                    cu = RoadTypeFlows(0, h) / MaxCap(0)
                    If cu >= CUCritValue Then
                        'add 2 lanes if necessary
                        RoadTypeLanesNew(0) += 2
                        AddedLanes(0) += 2
                        'write details to output file
                        newcapstring = FlowID & "," & (YearNum + 1) & ",0,2"
                        rlnc.WriteLine(newcapstring)
                        Exit Do
                    End If
                    h += 1
                Loop
            End If
            'check dual carriageways
            If RoadTypeLanesNew(1) > 0 Then
                h = 0
                Do While h < 24
                    cu = RoadTypeFlows(1, h) / MaxCap(1)
                    If cu >= CUCritValue Then
                        'add 2 lanes if necessary
                        RoadTypeLanesNew(1) += 2
                        AddedLanes(1) += 2
                        'write details to output file
                        newcapstring = FlowID & "," & (YearNum + 1) & ",1,2"
                        rlnc.WriteLine(newcapstring)
                        Exit Do
                    End If
                    h += 1
                Loop
            End If
            'check single carriageways
            If RoadTypeLanesNew(2) > 0 Then
                h = 0
                Do While h < 24
                    cu = RoadTypeFlows(2, h) / MaxCap(2)
                    If cu >= CUCritValue Then
                        'add 2 lanes if necessary
                        RoadTypeLanesNew(2) += 2
                        AddedLanes(2) += 2
                        'write details to output file
                        newcapstring = FlowID & "," & (YearNum + 1) & ",2,2"
                        rlnc.WriteLine(newcapstring)
                        Exit Do
                    End If
                    h += 1
                Loop
            End If
        End If

    End Sub

    Sub CapChange()

        'get old lane values
        TotalLanesOld = RoadTypeLanes(0) + RoadTypeLanes(1) + RoadTypeLanes(2)

        'get lane values for current year
        TotalLanesNew = RoadTypeLanesNew(0) + RoadTypeLanesNew(1) + RoadTypeLanesNew(2)

        'compare lanes in previous year with lanes in current year
        If TotalLanesOld = TotalLanesNew Then
            CapChangeNew = False
        Else
            CapChangeNew = True
        End If

    End Sub

End Module
