Module IntraZoneRoadModelA
    '1.3 this version adds in fuel consumption estimation
    'This version adds in a capacity constraint, and is dependent on module FullCDAM for file paths
    'It now also allows elasticities to vary over time, and reads them from an input file
    'also now includes the option to use variable elasticities
    'also now includes option to capture effect of smarter choices, smart logistics and smart urban logistics

    Dim RoadZoneInputData As IO.FileStream
    Dim riz As IO.StreamReader
    Dim RoadZoneExtInput As IO.FileStream
    Dim evz As IO.StreamReader
    Dim RoadZoneOutputData As IO.FileStream
    Dim roz As IO.StreamWriter
    Dim RoadZoneElasticities As IO.FileStream
    Dim rez As IO.StreamReader
    Dim stf As IO.StreamReader
    Dim ZoneInput As String
    Dim ZoneDetails() As String
    Dim ZoneID As Long
    Dim BaseVkm As Double
    Dim ZonePop As Long
    Dim ZoneGVA As Double
    Dim ZoneSpeed As Double
    Dim ZoneCarCost, ZoneLGVCost, ZoneHGV1Cost, ZoneHGV2Cost, ZonePSVCost As Double
    Dim ZoneExtVar(45, 90) As Double
    Dim NewVkm As Double
    Dim ZoneOutputRow As String
    Dim ZoneLaneKm(4) As Double
    Dim ZoneSpdNew As Double
    Dim RdZoneEl(9, 90) As String
    Dim RoadCatProb(4) As Double
    Dim VClass As String
    Dim FuelSpeed As Double
    Dim BaseSpeed As Double
    Dim FuelPerKm As Double
    Dim PetrolUsed As Double
    Dim DieselUsed As Double
    Dim ElectricUsed, LPGUsed, CNGUsed, HydrogenUsed As Double
    Dim BaseRVCatTraf(4, 5), RVCatTraf(4, 5), BaseRoadCatTraffic(4), RoadCatTraffic(4) As Double
    Dim VehTypeSplit(4, 5) As Double
    Dim RoadCatKm(4) As Double
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim PopRat(5), GVARat(5), SpdRat, PetRat(5), VkmRat(5) As Double
    Dim NewLaneKm As Double
    Dim SuppressedTraffic(4, 5) As Double
    Dim TheStArray(61) As String

    Public Sub RoadZoneMain()

        'read the input data for the zone
        ZoneSetFiles()

        'read in the elasticities
        Call ReadZoneElasticities()

        'loop through all the zones in the input file
        Do
            ZoneInput = riz.ReadLine

            'check if at end if file
            If ZoneInput Is Nothing Then
                Exit Do
            Else
                '1.3 get the strategy file
                SubStrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & SubStrategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
                stf = New IO.StreamReader(SubStrategyFile, System.Text.Encoding.Default)
                'read header row
                stf.ReadLine()

                'update the input variables
                Call LoadZoneInput()

                'get external variable values
                Call GetZoneExtVar()

                'apply zone equation to adjust demand
                Call RoadZoneKm(g_modelRunYear)

                'estimate fuel consumption
                Call RoadZoneFuelConsumption()

                'write output line with new demand figure
                Call RoadZoneOutput()

                'update base values
                Call NewBaseValues()

            End If
            stf.Close()
        Loop

        'Close input and output files
        riz.Close()
        evz.Close()
        roz.Close()
        rez.Close()

    End Sub

    Sub ZoneSetFiles()
        Dim row As String
        'This sub selects the input data files

        RoadZoneInputData = New IO.FileStream(DirPath & "RoadZoneInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        riz = New IO.StreamReader(RoadZoneInputData, System.Text.Encoding.Default)
        'read header row
        row = riz.ReadLine

        If UpdateExtVars = True Then
            If NewRdZCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If
        RoadZoneExtInput = New IO.FileStream(DirPath & EVFilePrefix & "RoadZoneExtVar" & EVFileSuffix & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        evz = New IO.StreamReader(RoadZoneExtInput, System.Text.Encoding.Default)
        'read header row
        row = evz.ReadLine

        RoadZoneOutputData = New IO.FileStream(DirPath & FilePrefix & "RoadZoneOutput.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        roz = New IO.StreamWriter(RoadZoneOutputData, System.Text.Encoding.Default)
        'write header row
        ZoneOutputRow = "ZoneID,Yeary,Vkmy,Spdy,Petroly,Diesely,Electricy,LPGy,CNGy,Hydrogeny"
        roz.WriteLine(ZoneOutputRow)

        RoadZoneElasticities = New IO.FileStream(DirPath & "RoadZoneElasticities.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rez = New IO.StreamReader(RoadZoneElasticities, System.Text.Encoding.Default)
        'read header row
        row = rez.ReadLine

    End Sub

    Sub LoadZoneInput()
        Dim SumProbKm As Double

        ZoneDetails = Split(ZoneInput, ",")
        ZoneID = ZoneDetails(0)
        BaseVkm = ZoneDetails(2)
        ZonePop = ZoneDetails(3)
        ZoneGVA = ZoneDetails(4)
        ZoneSpeed = ZoneDetails(5)
        ZoneCarCost = ZoneDetails(6)
        ZoneLGVCost = ZoneDetails(18)
        ZoneHGV1Cost = ZoneDetails(19)
        ZoneHGV2Cost = ZoneDetails(20)
        ZonePSVCost = ZoneDetails(21)
        ZoneLaneKm(1) = ZoneDetails(8)
        ZoneLaneKm(2) = CDbl(ZoneDetails(9)) + CDbl(ZoneDetails(10))
        ZoneLaneKm(3) = ZoneDetails(11)
        ZoneLaneKm(4) = CDbl(ZoneDetails(12)) + CDbl(ZoneDetails(13))
        RoadCatProb(1) = ZoneDetails(14)
        RoadCatProb(2) = ZoneDetails(15)
        RoadCatProb(3) = ZoneDetails(16)
        RoadCatProb(4) = ZoneDetails(17)
        BaseSpeed = ZoneSpeed
        'allocate the vkm to vehicle types and road types based on the initial proportions and on the proportion of traffic on different road types
        'convert lane km to road km for each road category, where Cat1 is motorways, Cat2 is rural A, Cat3 is rural minor and Cat4 is urban
        RoadCatKm(1) = ZoneDetails(8) / 6
        RoadCatKm(2) = (CDbl(ZoneDetails(9)) / 4) + (CDbl(ZoneDetails(10)) / 2)
        RoadCatKm(3) = ZoneDetails(11) / 2
        RoadCatKm(4) = (CDbl(ZoneDetails(12)) / 4) + (CDbl(ZoneDetails(13)) / 2)
        'the proportions come from DfT Traffic Statistics Table TRA0204 - see model guide for more details
        '**if there is a sudden decline in traffic then we probably just want to multiply by rcprob rather than by rcprob*rck/sumpkm
        SumProbKm = (RoadCatProb(1) * RoadCatKm(1)) + (RoadCatProb(2) * RoadCatKm(2)) + (RoadCatProb(3) * RoadCatKm(3)) + (RoadCatProb(4) * RoadCatKm(4))
        BaseRVCatTraf(1, 1) = (BaseVkm * ((RoadCatProb(1) * RoadCatKm(1)) / SumProbKm) * 0.755)
        BaseRVCatTraf(2, 1) = (BaseVkm * ((RoadCatProb(2) * RoadCatKm(2)) / SumProbKm) * 0.791)
        BaseRVCatTraf(3, 1) = (BaseVkm * ((RoadCatProb(3) * RoadCatKm(3)) / SumProbKm) * 0.794)
        BaseRVCatTraf(4, 1) = (BaseVkm * ((RoadCatProb(4) * RoadCatKm(4)) / SumProbKm) * 0.829)
        BaseRVCatTraf(1, 2) = (BaseVkm * ((RoadCatProb(1) * RoadCatKm(1)) / SumProbKm) * 0.123)
        BaseRVCatTraf(2, 2) = (BaseVkm * ((RoadCatProb(2) * RoadCatKm(2)) / SumProbKm) * 0.135)
        BaseRVCatTraf(3, 2) = (BaseVkm * ((RoadCatProb(3) * RoadCatKm(3)) / SumProbKm) * 0.174)
        BaseRVCatTraf(4, 2) = (BaseVkm * ((RoadCatProb(4) * RoadCatKm(4)) / SumProbKm) * 0.132)
        BaseRVCatTraf(1, 3) = (BaseVkm * ((RoadCatProb(1) * RoadCatKm(1)) / SumProbKm) * 0.037)
        BaseRVCatTraf(2, 3) = (BaseVkm * ((RoadCatProb(2) * RoadCatKm(2)) / SumProbKm) * 0.03)
        BaseRVCatTraf(3, 3) = (BaseVkm * ((RoadCatProb(3) * RoadCatKm(3)) / SumProbKm) * 0.019)
        BaseRVCatTraf(4, 3) = (BaseVkm * ((RoadCatProb(4) * RoadCatKm(4)) / SumProbKm) * 0.016)
        BaseRVCatTraf(1, 4) = (BaseVkm * ((RoadCatProb(1) * RoadCatKm(1)) / SumProbKm) * 0.081)
        BaseRVCatTraf(2, 4) = (BaseVkm * ((RoadCatProb(2) * RoadCatKm(2)) / SumProbKm) * 0.038)
        BaseRVCatTraf(3, 4) = (BaseVkm * ((RoadCatProb(3) * RoadCatKm(3)) / SumProbKm) * 0.005)
        BaseRVCatTraf(4, 4) = (BaseVkm * ((RoadCatProb(4) * RoadCatKm(4)) / SumProbKm) * 0.006)
        BaseRVCatTraf(1, 5) = (BaseVkm * ((RoadCatProb(1) * RoadCatKm(1)) / SumProbKm) * 0.004)
        BaseRVCatTraf(2, 5) = (BaseVkm * ((RoadCatProb(2) * RoadCatKm(2)) / SumProbKm) * 0.006)
        BaseRVCatTraf(3, 5) = (BaseVkm * ((RoadCatProb(3) * RoadCatKm(3)) / SumProbKm) * 0.009)
        BaseRVCatTraf(4, 5) = (BaseVkm * ((RoadCatProb(4) * RoadCatKm(4)) / SumProbKm) * 0.017)
        BaseRoadCatTraffic(1) = BaseRVCatTraf(1, 1) + BaseRVCatTraf(1, 2) + BaseRVCatTraf(1, 3) + BaseRVCatTraf(1, 4) + BaseRVCatTraf(1, 5)
        BaseRoadCatTraffic(2) = BaseRVCatTraf(2, 1) + BaseRVCatTraf(2, 2) + BaseRVCatTraf(2, 3) + BaseRVCatTraf(2, 4) + BaseRVCatTraf(2, 5)
        BaseRoadCatTraffic(3) = BaseRVCatTraf(3, 1) + BaseRVCatTraf(3, 2) + BaseRVCatTraf(3, 3) + BaseRVCatTraf(3, 4) + BaseRVCatTraf(3, 5)
        BaseRoadCatTraffic(4) = BaseRVCatTraf(4, 1) + BaseRVCatTraf(4, 2) + BaseRVCatTraf(4, 3) + BaseRVCatTraf(4, 4) + BaseRVCatTraf(4, 5)

    End Sub

    Sub GetZoneExtVar()
        Dim rownum As Long
        Dim row As String
        Dim ExtVarRow() As String
        Dim r As Byte
        Dim YearCheck As Integer

        rownum = 1
        YearCheck = 1
        Do While rownum < 91
            'loop through 90 rows in the external variables file, storing the values in the external variable values array
            row = evz.ReadLine
            ExtVarRow = Split(row, ",")
            If ExtVarRow(1) = YearCheck Then
                'as long as the year counter corresponds to the year value in the input data, write values to the array
                For r = 1 To 45
                    ZoneExtVar(r, rownum) = ExtVarRow(r)
                Next
                rownum += 1
            Else
                'otherwise stop the model and write an error to the log file
                Dim msg As String
                msg = "ERROR in intrazonal road model Sub GetZoneExtVar - year counter does not correspond to year value in input data for Zone " & ZoneID & " in year " & g_modelRunYear
                logarray(1, 0) = g_modelrunID : logarray(1, 1) = 1 : logarray(1, 2) = msg
                Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
                logarray(1, 0) = g_modelrunID : logarray(1, 1) = 1 : logarray(1, 2) = "Model run prematurely terminated at" & System.DateTime.Now
                Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
                ErrorLog(ErrorSeverity.FATAL, "Mathematical", "GetZoneExtVar", msg)
                Throw New System.Exception(msg)
            End If
            YearCheck += 1
        Loop
    End Sub

    Sub ReadZoneElasticities()
        Dim row As String
        Dim elstring() As String
        Dim yearcheck As Integer
        Dim elcount As Integer

        yearcheck = 1

        Do
            'read in row from elasticities file
            row = rez.ReadLine
            If row Is Nothing Then
                Exit Do
            End If
            'split it into array - 1 is passpop, 2 is passgva, 3 is passspeed, 4 is passcost, 5 is spdcapu, 6 is frtpop, 7 is frtgva, 8 is frtspd, 9 is frtcost
            elstring = Split(row, ",")
            elcount = 1
            Do While elcount < 10
                RdZoneEl(elcount, yearcheck) = elstring(elcount)
                elcount += 1
            Loop
            yearcheck += 1
        Loop

    End Sub

    Sub RoadZoneKm(Year As Integer)
        'v1.3 now calculate traffic separately for the different road types (this is to allow the fuel consumption calculations to work with changes in fuel mix and vehicle mix over time)
        Dim rdtype As Integer
        Dim YearCount As Integer

        YearCount = g_modelRunYear - g_initialYear

        'now incorporates variable elasticities - only do this here if we are not using them - otherwise do it in a separate sub
        If VariableEl = False Then
            'Calculate the values of the various input ratios for the different types of road vehicle (speed assumed to be the same for all)

            PopRat(1) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(1, YearCount)
            PopRat(2) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(6, YearCount)
            PopRat(3) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(6, YearCount)
            PopRat(4) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(6, YearCount)
            PopRat(5) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(1, YearCount)
            GVARat(1) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(2, YearCount)
            GVARat(2) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(7, YearCount)
            GVARat(3) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(7, YearCount)
            GVARat(4) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(7, YearCount)
            GVARat(5) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(2, YearCount)
            SpdRat = (ZoneSpeed / ZoneSpeed) ^ RdZoneEl(3, YearCount)
            'calculate the ratio for different types of road vehicle
            PetRat(1) = (ZoneExtVar(4, YearCount) / ZoneCarCost) ^ RdZoneEl(4, YearCount)
            PetRat(2) = (ZoneExtVar(42, YearCount) / ZoneLGVCost) ^ RdZoneEl(9, YearCount)
            PetRat(3) = (ZoneExtVar(43, YearCount) / ZoneHGV1Cost) ^ RdZoneEl(9, YearCount)
            PetRat(4) = (ZoneExtVar(44, YearCount) / ZoneHGV2Cost) ^ RdZoneEl(9, YearCount)
            PetRat(5) = (ZoneExtVar(45, YearCount) / ZonePSVCost) ^ RdZoneEl(4, YearCount)

            'Combine these ratios to get the vkm ratios
            For x = 1 To 5
                VkmRat(x) = PopRat(x) * GVARat(x) * SpdRat * PetRat(x)
            Next
        End If

        'loop through the four road types calculating the traffic levels
        For rdtype = 1 To 4
            'check if there is any of that road type in the zone
            Select Case rdtype
                Case 1
                    NewLaneKm = ZoneExtVar(6, YearCount)
                Case 2
                    NewLaneKm = CDbl(ZoneExtVar(7, YearCount)) + CDbl(ZoneExtVar(8, YearCount))
                Case 3
                    NewLaneKm = ZoneExtVar(9, YearCount)
                Case 4
                    NewLaneKm = CDbl(ZoneExtVar(10, YearCount)) + CDbl(ZoneExtVar(11, YearCount))
            End Select
            If NewLaneKm > 0 Then
                'Multiply the vkm ratio by the previous year's vkm to get new vkm figures
                If VariableEl = True Then
                    'if using variable elasticities then calculate these in a separate sub - if not then they were calculated at the start of this sub
                    Call GetVariableElasticities()
                End If
                For x = 1 To 5
                    RVCatTraf(rdtype, x) = BaseRVCatTraf(rdtype, x) * VkmRat(x)
                Next

                RoadCatTraffic(rdtype) = RVCatTraf(rdtype, 1) + RVCatTraf(rdtype, 2) + RVCatTraf(rdtype, 3) + RVCatTraf(rdtype, 4) + RVCatTraf(rdtype, 5)

                'v1.3 mod - if using smarter choices, smart logistics or urban freight innovations we only set the vehicle type splits in the first year, to avoid car traffic share declining away to nothing
                If SmarterChoices = True Then
                    If YearCount = 1 Then
                        'set the vehicle type splits
                        For x = 1 To 5
                            VehTypeSplit(rdtype, x) = RVCatTraf(rdtype, x) / RoadCatTraffic(rdtype)
                        Next
                    End If
                ElseIf UrbanFrt = True Then
                    If YearCount = 1 Then
                        'set the vehicle type splits
                        For x = 1 To 5
                            VehTypeSplit(rdtype, x) = RVCatTraf(rdtype, x) / RoadCatTraffic(rdtype)
                        Next
                    End If
                ElseIf SmartFrt = True Then
                    If YearCount = 1 Then
                        'set the vehicle type splits
                        For x = 1 To 5
                            VehTypeSplit(rdtype, x) = RVCatTraf(rdtype, x) / RoadCatTraffic(rdtype)
                        Next
                    End If
                Else
                    'set the vehicle type splits
                    For x = 1 To 5
                        VehTypeSplit(rdtype, x) = RVCatTraf(rdtype, x) / RoadCatTraffic(rdtype)
                    Next
                End If

                'Set up new road km variable from external variables and calculate resulting change in speed
                'now includes variable elasticity option
                If VariableEl = True Then
                    OldX = ZoneSpeed
                    OldY = BaseRoadCatTraffic(rdtype) / NewLaneKm
                    NewY = RoadCatTraffic(rdtype) / NewLaneKm
                    If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                        OldEl = RdZoneEl(5, YearCount)
                        Call VarElCalc()
                        SpdRat = VarRat
                    Else
                        SpdRat = ((RoadCatTraffic(rdtype) / NewLaneKm) / (BaseRoadCatTraffic(rdtype) / NewLaneKm)) ^ RdZoneEl(5, YearCount)
                    End If
                Else
                    SpdRat = ((RoadCatTraffic(rdtype) / NewLaneKm) / (BaseRoadCatTraffic(rdtype) / NewLaneKm)) ^ RdZoneEl(5, YearCount)
                End If
                ZoneSpdNew = SpdRat * ZoneSpeed

                'iterate between calculation of speed and vkm ratios unti convergence reached
                Do Until SpdRat >= 0.999 And SpdRat <= 1.001
                    'set the base vkm to equal the previous new vkm
                    BaseRoadCatTraffic(rdtype) = RoadCatTraffic(rdtype)
                    'recalculate the vehicle km figure
                    'now includes variable elasticity option
                    If VariableEl = True Then
                        OldX = BaseRoadCatTraffic(rdtype)
                        OldY = ZoneSpeed
                        NewY = ZoneSpdNew
                        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                            OldEl = RdZoneEl(3, YearCount)
                            Call VarElCalc()
                            SpdRat = VarRat
                        Else
                            SpdRat = (ZoneSpdNew / ZoneSpeed) ^ RdZoneEl(3, YearCount)
                        End If
                    Else
                        SpdRat = (ZoneSpdNew / ZoneSpeed) ^ RdZoneEl(3, YearCount)
                    End If
                    RoadCatTraffic(rdtype) = SpdRat * BaseRoadCatTraffic(rdtype)
                    'set the base speed to equal the previous new speed
                    ZoneSpeed = ZoneSpdNew
                    'calculate the resulting change in speed from the new vehicle km figure
                    'now includes variable elasticity option
                    If VariableEl = True Then
                        OldX = ZoneSpeed
                        OldY = BaseRoadCatTraffic(rdtype) / NewLaneKm
                        NewY = RoadCatTraffic(rdtype) / NewLaneKm
                        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                            OldEl = RdZoneEl(5, YearCount)
                            Call VarElCalc()
                            SpdRat = VarRat
                        Else
                            SpdRat = ((RoadCatTraffic(rdtype) / NewLaneKm) / (BaseRoadCatTraffic(rdtype) / NewLaneKm)) ^ RdZoneEl(5, YearCount)
                        End If
                    Else
                        SpdRat = ((RoadCatTraffic(rdtype) / NewLaneKm) / (BaseRoadCatTraffic(rdtype) / NewLaneKm)) ^ RdZoneEl(5, YearCount)
                    End If
                    ZoneSpdNew = SpdRat * ZoneSpeed
                Loop

                'split the final vkm figure between vehicle types
                For x = 1 To 5
                    RVCatTraf(rdtype, x) = RoadCatTraffic(rdtype) * VehTypeSplit(rdtype, x)
                Next

                'v1.3 if using smarter choices then scale the urban car traffic accordingly
                If SmarterChoices = True Then
                    'check if we are after the date of introduction
                    If SmartIntro < YearCount Then
                        'if so then subtract the unscaled urban car traffic from the total urban traffic
                        'need to store the suppressed traffic, as otherwise the model will keep suppressing demand by the set % each year, leading to a much greater cumulative decay than anticipated
                        RoadCatTraffic(4) = RoadCatTraffic(4) - RVCatTraf(4, 1)
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
                        RoadCatTraffic(4) += RVCatTraf(4, 1)
                    End If
                End If

                'v1.3 if using urban freight innovations then scale the urban LGV/HGV traffic accordingly
                If UrbanFrt = True Then
                    'check if we are after the date of introduction
                    If UrbFrtIntro < YearCount Then
                        'if so then subtract the unscaled urban LGV/HGV traffic from the total urban traffic
                        'need to store the suppressed traffic, as otherwise the model will keep suppressing demand by the set % each year, leading to a much greater cumulative decay than anticipated
                        RoadCatTraffic(4) = RoadCatTraffic(4) - (RVCatTraf(4, 2) + RVCatTraf(4, 3) + RVCatTraf(4, 4))
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
                        RoadCatTraffic(4) = RoadCatTraffic(4) + RVCatTraf(4, 2) + RVCatTraf(4, 3) + RVCatTraf(4, 4)
                    End If
                End If

                'v1.3 if using smart logistics then scale the non-urban HGV traffic accordingly
                If SmartFrt = True Then
                    'check if we are after the date of introduction
                    If SmFrtIntro < YearCount Then
                        'if so then subtract the unscaled non-urban HGV traffic from the total urban traffic
                        'need to store the suppressed traffic, as otherwise the model will keep suppressing demand by the set % each year, leading to a much greater cumulative decay than anticipated
                        For x = 1 To 3
                            RoadCatTraffic(x) = RoadCatTraffic(x) - (RVCatTraf(x, 3) + RVCatTraf(x, 4))
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
                            RoadCatTraffic(x) = RoadCatTraffic(x) + RVCatTraf(x, 3) + RVCatTraf(x, 4)
                        Next
                    End If
                End If

            Else
                RoadCatTraffic(rdtype) = 0
                For x = 1 To 5
                    RVCatTraf(rdtype, x) = 0
                Next
            End If
        Next

        'calculate the total vkm figure

        NewVkm = RoadCatTraffic(1) + RoadCatTraffic(2) + RoadCatTraffic(3) + RoadCatTraffic(4)

    End Sub

    Sub GetVariableElasticities()
        Dim YearCount As Integer

        YearCount = g_modelRunYear - g_initialYear

        'Calculate the values of the various input ratios for the different types of road vehicle (speed assumed to be the same for all)
        OldX = NewLaneKm
        'pop1ratio
        OldY = ZonePop
        NewY = ZoneExtVar(2, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(1, YearCount)
            Call VarElCalc()
            PopRat(1) = VarRat
        Else
            PopRat(1) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(1, YearCount)
        End If
        'pop2ratio
        OldY = ZonePop
        NewY = ZoneExtVar(2, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(6, YearCount)
            Call VarElCalc()
            PopRat(2) = VarRat
        Else
            PopRat(2) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(6, YearCount)
        End If
        'pop3ratio
        OldY = ZonePop
        NewY = ZoneExtVar(2, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(6, YearCount)
            Call VarElCalc()
            PopRat(3) = VarRat
        Else
            PopRat(3) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(6, YearCount)
        End If
        'pop4ratio
        OldY = ZonePop
        NewY = ZoneExtVar(2, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(6, YearCount)
            Call VarElCalc()
            PopRat(4) = VarRat
        Else
            PopRat(4) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(6, YearCount)
        End If
        'pop5ratio
        OldY = ZonePop
        NewY = ZoneExtVar(2, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(1, YearCount)
            Call VarElCalc()
            PopRat(5) = VarRat
        Else
            PopRat(5) = (ZoneExtVar(2, YearCount) / ZonePop) ^ RdZoneEl(1, YearCount)
        End If
        'gva1ratio
        OldY = ZoneGVA
        NewY = ZoneExtVar(3, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(2, YearCount)
            Call VarElCalc()
            GVARat(1) = VarRat
        Else
            GVARat(1) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(2, YearCount)
        End If
        'gva2ratio
        OldY = ZoneGVA
        NewY = ZoneExtVar(3, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(7, YearCount)
            Call VarElCalc()
            GVARat(2) = VarRat
        Else
            GVARat(2) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(7, YearCount)
        End If
        'gva3ratio
        OldY = ZoneGVA
        NewY = ZoneExtVar(3, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(7, YearCount)
            Call VarElCalc()
            GVARat(3) = VarRat
        Else
            GVARat(3) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(7, YearCount)
        End If
        'gva4ratio
        OldY = ZoneGVA
        NewY = ZoneExtVar(3, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(7, YearCount)
            Call VarElCalc()
            GVARat(4) = VarRat
        Else
            GVARat(4) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(7, YearCount)
        End If
        'gva5ratio
        OldY = ZoneGVA
        NewY = ZoneExtVar(3, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(7, YearCount)
            Call VarElCalc()
            GVARat(5) = VarRat
        Else
            GVARat(5) = (ZoneExtVar(3, YearCount) / ZoneGVA) ^ RdZoneEl(2, YearCount)
        End If
        'speed ratio - this is constant
        SpdRat = (ZoneSpeed / ZoneSpeed) ^ RdZoneEl(3, YearCount)
        'cost1ratio
        OldY = ZoneCarCost
        NewY = ZoneExtVar(4, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(4, YearCount)
            Call VarElCalc()
            PetRat(1) = VarRat
        Else
            PetRat(1) = (ZoneExtVar(4, YearCount) / ZoneCarCost) ^ RdZoneEl(4, YearCount)
        End If
        'cost2ratio
        OldY = ZoneLGVCost
        NewY = ZoneExtVar(42, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(9, YearCount)
            Call VarElCalc()
            PetRat(2) = VarRat
        Else
            PetRat(2) = (ZoneExtVar(42, YearCount) / ZoneLGVCost) ^ RdZoneEl(9, YearCount)
        End If
        'cost3ratio
        OldY = ZoneHGV1Cost
        NewY = ZoneExtVar(43, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(9, YearCount)
            Call VarElCalc()
            PetRat(3) = VarRat
        Else
            PetRat(3) = (ZoneExtVar(43, YearCount) / ZoneHGV1Cost) ^ RdZoneEl(9, YearCount)
        End If
        'cost4ratio
        OldY = ZoneHGV2Cost
        NewY = ZoneExtVar(44, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(9, YearCount)
            Call VarElCalc()
            PetRat(4) = VarRat
        Else
            PetRat(4) = (ZoneExtVar(44, YearCount) / ZoneHGV2Cost) ^ RdZoneEl(9, YearCount)
        End If
        'cost5ratio
        OldY = ZonePSVCost
        NewY = ZoneExtVar(45, YearCount)
        If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
            OldEl = RdZoneEl(4, YearCount)
            Call VarElCalc()
            PetRat(5) = VarRat
        Else
            PetRat(5) = (ZoneExtVar(45, YearCount) / ZonePSVCost) ^ RdZoneEl(4, YearCount)
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
        Dim CatCount As Integer
        Dim RVFCatTraf(4, 5, 10) As Double
        Dim RVFFuel(4, 5, 10) As Double
        Dim VCount As Integer
        Dim StratLine As String
        Dim YearCount As Integer

        YearCount = g_modelRunYear - g_initialYear

        'split the vehicle km by vehicle category data into fuel types, where fuel 1 is petrol, fuel 2 is diesel, fuel 3 is petrol hybrid, fuel 4 is diesel hybrid, fuel 5 is plug-in hybrid,
        'fuel 6 is battery electric, fuel 7 is LPG, fuel 8 is CNG, fuel 9 is hydrogen ICE and fuel 10 is hydrogen fuel cell
        'note that for PHEVs urban roads will use electricity, whereas other roads will use petrol/diesel
        For CatCount = 1 To 4
            RVFCatTraf(CatCount, 1, 1) = RVCatTraf(CatCount, 1) * ZoneExtVar(12, YearCount)
            RVFCatTraf(CatCount, 1, 2) = RVCatTraf(CatCount, 1) * ZoneExtVar(13, YearCount)
            RVFCatTraf(CatCount, 1, 3) = RVCatTraf(CatCount, 1) * ZoneExtVar(25, YearCount)
            RVFCatTraf(CatCount, 1, 4) = RVCatTraf(CatCount, 1) * ZoneExtVar(26, YearCount)
            RVFCatTraf(CatCount, 1, 5) = RVCatTraf(CatCount, 1) * ZoneExtVar(27, YearCount)
            RVFCatTraf(CatCount, 1, 6) = RVCatTraf(CatCount, 1) * ZoneExtVar(14, YearCount)
            RVFCatTraf(CatCount, 1, 9) = RVCatTraf(CatCount, 1) * ZoneExtVar(28, YearCount)
            RVFCatTraf(CatCount, 1, 10) = RVCatTraf(CatCount, 1) * ZoneExtVar(29, YearCount)
            RVFCatTraf(CatCount, 2, 1) = RVCatTraf(CatCount, 2) * ZoneExtVar(15, YearCount)
            RVFCatTraf(CatCount, 2, 2) = RVCatTraf(CatCount, 2) * ZoneExtVar(16, YearCount)
            RVFCatTraf(CatCount, 2, 4) = RVCatTraf(CatCount, 2) * ZoneExtVar(30, YearCount)
            RVFCatTraf(CatCount, 2, 5) = RVCatTraf(CatCount, 2) * ZoneExtVar(31, YearCount)
            RVFCatTraf(CatCount, 2, 6) = RVCatTraf(CatCount, 2) * ZoneExtVar(17, YearCount)
            RVFCatTraf(CatCount, 2, 7) = RVCatTraf(CatCount, 2) * ZoneExtVar(32, YearCount)
            RVFCatTraf(CatCount, 2, 8) = RVCatTraf(CatCount, 2) * ZoneExtVar(33, YearCount)
            RVFCatTraf(CatCount, 3, 2) = RVCatTraf(CatCount, 3) * ZoneExtVar(18, YearCount)
            RVFCatTraf(CatCount, 3, 4) = RVCatTraf(CatCount, 3) * ZoneExtVar(39, YearCount)
            RVFCatTraf(CatCount, 3, 9) = RVCatTraf(CatCount, 3) * ZoneExtVar(40, YearCount)
            RVFCatTraf(CatCount, 3, 10) = RVCatTraf(CatCount, 3) * ZoneExtVar(41, YearCount)
            RVFCatTraf(CatCount, 4, 2) = RVCatTraf(CatCount, 4) * ZoneExtVar(18, YearCount)
            RVFCatTraf(CatCount, 4, 4) = RVCatTraf(CatCount, 4) * ZoneExtVar(39, YearCount)
            RVFCatTraf(CatCount, 4, 9) = RVCatTraf(CatCount, 4) * ZoneExtVar(40, YearCount)
            RVFCatTraf(CatCount, 4, 10) = RVCatTraf(CatCount, 4) * ZoneExtVar(41, YearCount)
            RVFCatTraf(CatCount, 5, 2) = RVCatTraf(CatCount, 5) * ZoneExtVar(20, YearCount)
            RVFCatTraf(CatCount, 5, 4) = RVCatTraf(CatCount, 5) * ZoneExtVar(34, YearCount)
            RVFCatTraf(CatCount, 5, 5) = RVCatTraf(CatCount, 5) * ZoneExtVar(35, YearCount)
            RVFCatTraf(CatCount, 5, 6) = RVCatTraf(CatCount, 5) * ZoneExtVar(21, YearCount)
            RVFCatTraf(CatCount, 5, 7) = RVCatTraf(CatCount, 5) * ZoneExtVar(36, YearCount)
            RVFCatTraf(CatCount, 5, 8) = RVCatTraf(CatCount, 5) * ZoneExtVar(37, YearCount)
            RVFCatTraf(CatCount, 5, 10) = RVCatTraf(CatCount, 5) * ZoneExtVar(38, YearCount)
        Next

        'read line from strategy file (to get fuel efficiency changes
        StratLine = stf.ReadLine
        TheStArray = Split(StratLine, ",")

        'estimate fuel consumption for each vehicle type
        'initial average speeds taken from tables in model description document - but this will need to change year on year to reflect changes in congestion
        'Petrol cars
        VClass = "CarP"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 1) = RVFCatTraf(1, 1, 1) * FuelPerKm * TheStArray(31)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (75.639 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 1) = RVFCatTraf(2, 1, 1) * FuelPerKm * TheStArray(31)
        Else
            RVFFuel(2, 1, 1) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 1) = RVFCatTraf(3, 1, 1) * FuelPerKm * TheStArray(31)
        'urban
        FuelSpeed = 52.143 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 1, 1) = RVFCatTraf(4, 1, 1) * FuelPerKm * TheStArray(31)
        'Diesel cars
        VClass = "CarD"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 2) = RVFCatTraf(1, 1, 2) * FuelPerKm * TheStArray(32)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (75.639 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 2) = RVFCatTraf(2, 1, 2) * FuelPerKm * TheStArray(32)
        Else
            RVFFuel(2, 1, 2) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 2) = RVFCatTraf(3, 1, 2) * FuelPerKm * TheStArray(32)
        'urban
        FuelSpeed = 52.143 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 1, 2) = RVFCatTraf(4, 1, 2) * FuelPerKm * TheStArray(32)
        'Petrol hybrid cars - these are being calculated based on a proportional adjustment of the petrol fuel consumption figures (ie dividing the Brand hybrid figure by the Brand petrol figure and then multiplying by the DfT petrol figure)
        VClass = "CarP"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 3) = RVFCatTraf(1, 1, 3) * (FuelPerKm * (11.2 / 18.6)) * TheStArray(43)
        'rural a
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (75.639 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 3) = RVFCatTraf(2, 1, 3) * (FuelPerKm * (11.2 / 18.6)) * TheStArray(43)
        Else
            RVFFuel(2, 1, 3) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 3) = RVFCatTraf(3, 1, 3) * (FuelPerKm * (11.2 / 18.6)) * TheStArray(43)
        'urban
        FuelSpeed = 52.143 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 1, 3) = RVFCatTraf(4, 1, 3) * (FuelPerKm * (11.2 / 18.6)) * TheStArray(43)
        'Diesel hybrid cars  - these are being calculated based on a proportional adjustment of the diesel fuel consumption figures (ie dividing the Brand hybrid figure by the Brand diesel figure and then multiplying by the DfT diesel figure)
        VClass = "CarD"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 4) = RVFCatTraf(1, 1, 4) * (FuelPerKm * (7.5 / 12.4)) * TheStArray(44)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (75.639 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 4) = RVFCatTraf(2, 1, 4) * (FuelPerKm * (7.5 / 12.4)) * TheStArray(44)
        Else
            RVFFuel(2, 1, 4) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 4) = RVFCatTraf(3, 1, 4) * (FuelPerKm * (7.5 / 12.4)) * TheStArray(44)
        'urban
        FuelSpeed = 52.143 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 1, 4) = RVFCatTraf(4, 1, 4) * (FuelPerKm * (7.5 / 12.4)) * TheStArray(44)
        'Plug-in hybrid cars - for rural driving these use a proportional adjustment of the Brand figures (petrol/diesel), whereas for urban driving they use the Brand electric figures
        VClass = "CarP"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 1, 5) = RVFCatTraf(1, 1, 5) * (FuelPerKm * (18.1 / 25.9)) * TheStArray(45)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 1) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (75.639 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 1, 5) = RVFCatTraf(2, 1, 5) * (FuelPerKm * (18.1 / 25.9)) * TheStArray(45)
        Else
            RVFFuel(2, 1, 5) = 0
        End If
        'rural minor
        FuelSpeed = 75.639 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 1, 5) = RVFCatTraf(3, 1, 5) * (FuelPerKm * (18.1 / 25.9)) * TheStArray(45)
        'urban
        RVFFuel(4, 1, 5) = RVFCatTraf(4, 1, 5) * 0.1557 * TheStArray(45)
        'Battery electric cars - fuel consumption figure now taken from Brand (2010)
        'motorway
        RVFFuel(1, 1, 6) = RVFCatTraf(1, 1, 6) * 0.165 * TheStArray(33)
        'rural a
        RVFFuel(2, 1, 6) = RVFCatTraf(2, 1, 6) * 0.165 * TheStArray(33)
        'rural minor
        RVFFuel(3, 1, 6) = RVFCatTraf(3, 1, 6) * 0.165 * TheStArray(33)
        'urban
        RVFFuel(4, 1, 6) = RVFCatTraf(4, 1, 6) * 0.165 * TheStArray(33)
        'hydrogen ICE cars - fuel consumption figure from Brand (2010)
        'motorway
        RVFFuel(1, 1, 9) = RVFCatTraf(1, 1, 9) * 0.438 * TheStArray(46)
        'rural a
        RVFFuel(2, 1, 9) = RVFCatTraf(2, 1, 9) * 0.438 * TheStArray(46)
        'rural minor
        RVFFuel(3, 1, 9) = RVFCatTraf(3, 1, 9) * 0.438 * TheStArray(46)
        'urban
        RVFFuel(4, 1, 9) = RVFCatTraf(4, 1, 9) * 0.438 * TheStArray(46)
        'hydrogen fuel cell cars - fuel consumption figure from Brand (2010)
        'motorway
        RVFFuel(1, 1, 10) = RVFCatTraf(1, 1, 10) * 0.1777 * TheStArray(47)
        'rural a
        RVFFuel(2, 1, 10) = RVFCatTraf(2, 1, 10) * 0.1777 * TheStArray(47)
        'rural minor
        RVFFuel(3, 1, 10) = RVFCatTraf(3, 1, 10) * 0.1777 * TheStArray(47)
        'urban
        RVFFuel(4, 1, 10) = RVFCatTraf(4, 1, 10) * 0.1777 * TheStArray(47)

        'Petrol LGVs
        VClass = "LGVP"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 2, 1) = RVFCatTraf(1, 2, 1) * FuelPerKm * TheStArray(34)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 2) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (77.249 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 2, 1) = RVFCatTraf(2, 2, 1) * FuelPerKm * TheStArray(34)
        Else
            RVFFuel(2, 2, 1) = 0
        End If
        'rural minor
        FuelSpeed = 77.249 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 2, 1) = RVFCatTraf(3, 2, 1) * FuelPerKm * TheStArray(34)
        'urban
        FuelSpeed = 52.786 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 2, 1) = RVFCatTraf(4, 2, 1) * FuelPerKm * TheStArray(34)
        'Diesel LGVs
        VClass = "LGVD"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 2, 2) = RVFCatTraf(1, 2, 2) * FuelPerKm * TheStArray(35)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 2) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (77.249 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 2, 2) = RVFCatTraf(2, 2, 2) * FuelPerKm * TheStArray(35)
        Else
            RVFFuel(2, 2, 2) = 0
        End If
        'rural minor
        FuelSpeed = 77.249 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 2, 2) = RVFCatTraf(3, 2, 2) * FuelPerKm * TheStArray(35)
        'urban
        FuelSpeed = 52.786 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 2, 2) = RVFCatTraf(4, 2, 2) * FuelPerKm * TheStArray(35)
        'diesel hybrid LGVs
        VClass = "LGVD"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 2, 4) = RVFCatTraf(1, 2, 4) * (FuelPerKm * (4.4 / 7.9)) * TheStArray(48)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 2) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (77.249 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 2, 4) = RVFCatTraf(2, 2, 4) * (FuelPerKm * (4.4 / 7.9)) * TheStArray(48)
        Else
            RVFFuel(2, 2, 4) = 0
        End If
        'rural minor
        FuelSpeed = 77.249 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 2, 4) = RVFCatTraf(3, 2, 4) * (FuelPerKm * (4.4 / 7.9)) * TheStArray(48)
        'urban
        FuelSpeed = 52.786 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 2, 4) = RVFCatTraf(4, 2, 4) * (FuelPerKm * (4.4 / 7.9)) * TheStArray(48)
        'plug-in hybrid LGVs - for rural driving these use a proportional adjustment of the Brand figures (petrol/diesel), whereas for urban driving they use the Brand electric figures
        VClass = "LGVD"
        'motorway
        FuelSpeed = 111.04 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 2, 5) = RVFCatTraf(1, 2, 5) * (FuelPerKm * (5.8 / 7.9)) * TheStArray(49)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 2) > 0 Then
            FuelSpeed = (((109.44 * ZoneExtVar(7, YearCount)) + (77.249 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 2, 5) = RVFCatTraf(2, 2, 5) * (FuelPerKm * (5.8 / 7.9)) * TheStArray(49)
        Else
            RVFFuel(2, 2, 5) = 0
        End If
        'rural minor
        FuelSpeed = 77.249 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 2, 5) = RVFCatTraf(3, 2, 5) * (FuelPerKm * (5.8 / 7.9)) * TheStArray(49)
        'urban
        RVFFuel(4, 2, 5) = RVFCatTraf(4, 2, 5) * 0.423 * TheStArray(49)
        'Battery Electric LGVs - fuel consumption figure now from Brand (2010)
        'motorway
        RVFFuel(1, 2, 6) = RVFCatTraf(1, 2, 6) * 0.562 * TheStArray(36)
        'rural a
        RVFFuel(2, 2, 6) = RVFCatTraf(2, 2, 6) * 0.562 * TheStArray(36)
        'rural minor
        RVFFuel(3, 2, 6) = RVFCatTraf(3, 2, 6) * 0.562 * TheStArray(36)
        'urban
        RVFFuel(4, 2, 6) = RVFCatTraf(4, 2, 6) * 0.562 * TheStArray(36)
        'LPG LGVs
        'motorway
        RVFFuel(1, 2, 7) = RVFCatTraf(1, 2, 7) * 0.118 * TheStArray(50)
        'rural a
        RVFFuel(2, 2, 7) = RVFCatTraf(2, 2, 7) * 0.118 * TheStArray(50)
        'rural minor
        RVFFuel(3, 2, 7) = RVFCatTraf(3, 2, 7) * 0.118 * TheStArray(50)
        'urban
        RVFFuel(4, 2, 7) = RVFCatTraf(4, 2, 7) * 0.118 * TheStArray(50)
        'CNG LGVs
        'motorway
        RVFFuel(1, 2, 8) = RVFCatTraf(1, 2, 8) * 0.808 * TheStArray(51)
        'rural a
        RVFFuel(2, 2, 8) = RVFCatTraf(2, 2, 8) * 0.808 * TheStArray(51)
        'rural minor
        RVFFuel(3, 2, 8) = RVFCatTraf(3, 2, 8) * 0.808 * TheStArray(51)
        'urban
        RVFFuel(4, 2, 8) = RVFCatTraf(4, 2, 8) * 0.808 * TheStArray(51)

        'Diesel  2-3 axle rigid HGVs
        VClass = "HGV1D"
        'motorway
        FuelSpeed = 92.537 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 3, 2) = RVFCatTraf(1, 3, 2) * FuelPerKm * TheStArray(37)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 3) > 0 Then
            FuelSpeed = (((90.928 * ZoneExtVar(7, YearCount)) + (70.811 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 3, 2) = RVFCatTraf(2, 3, 2) * FuelPerKm * TheStArray(37)
        Else
            RVFFuel(2, 3, 2) = 0
        End If
        'rural minor
        FuelSpeed = 70.811 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 3, 2) = RVFCatTraf(3, 3, 2) * FuelPerKm * TheStArray(37)
        'urban
        FuelSpeed = 51.579 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 3, 2) = RVFCatTraf(4, 3, 2) * FuelPerKm * TheStArray(37)
        'diesel hybrid small HGVs
        VClass = "HGV1D"
        'motorway
        FuelSpeed = 92.537 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 3, 4) = RVFCatTraf(1, 3, 4) * (FuelPerKm * (15 / 25.9)) * TheStArray(57)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 3) > 0 Then
            FuelSpeed = (((90.928 * ZoneExtVar(7, YearCount)) + (70.811 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 3, 4) = RVFCatTraf(2, 3, 4) * (FuelPerKm * (15 / 25.9)) * TheStArray(57)
        Else
            RVFFuel(2, 3, 4) = 0
        End If
        'rural minor
        FuelSpeed = 70.811 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 3, 4) = RVFCatTraf(3, 3, 4) * (FuelPerKm * (15 / 25.9)) * TheStArray(57)
        'urban
        FuelSpeed = 51.579 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 3, 4) = RVFCatTraf(4, 3, 4) * (FuelPerKm * (15 / 25.9)) * TheStArray(57)
        'hydrogen ICE small HGVs
        'motorway
        RVFFuel(1, 3, 9) = RVFCatTraf(1, 3, 9) * 0.957 * TheStArray(58)
        'rural a
        RVFFuel(2, 3, 9) = RVFCatTraf(2, 3, 9) * 0.957 * TheStArray(58)
        'rural minor
        RVFFuel(3, 3, 9) = RVFCatTraf(3, 3, 9) * 0.957 * TheStArray(58)
        'urban
        RVFFuel(4, 3, 9) = RVFCatTraf(4, 3, 9) * 0.957 * TheStArray(58)
        'hydrogen fuel cell small HGVs
        'motorway
        RVFFuel(1, 3, 10) = RVFCatTraf(1, 3, 10) * 0.898 * TheStArray(59)
        'rural a
        RVFFuel(2, 3, 10) = RVFCatTraf(2, 3, 10) * 0.898 * TheStArray(59)
        'rural minor
        RVFFuel(3, 3, 10) = RVFCatTraf(3, 3, 10) * 0.898 * TheStArray(59)
        'urban
        RVFFuel(4, 3, 10) = RVFCatTraf(4, 3, 10) * 0.898 * TheStArray(59)

        'Diesel 4+ axle rigid and artic HGVs
        VClass = "HGV2D"
        'motorway
        FuelSpeed = 86.905 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 4, 2) = RVFCatTraf(1, 4, 2) * FuelPerKm * TheStArray(39)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 4) > 0 Then
            FuelSpeed = (((85.295 * ZoneExtVar(7, YearCount)) + (69.685 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 4, 2) = RVFCatTraf(2, 4, 2) * FuelPerKm * TheStArray(39)
        Else
            RVFFuel(2, 4, 2) = 0
        End If
        'rural minor
        FuelSpeed = 69.685 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 4, 2) = RVFCatTraf(3, 4, 2) * FuelPerKm * TheStArray(39)
        'urban
        FuelSpeed = 53.511 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 4, 2) = RVFCatTraf(4, 4, 2) * FuelPerKm * TheStArray(39)
        'diesel hybrid large HGVs
        VClass = "HGV2D"
        'motorway
        FuelSpeed = 86.905 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 4, 4) = RVFCatTraf(1, 4, 4) * (FuelPerKm * (22.1 / 37.6)) * TheStArray(60)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 4) > 0 Then
            FuelSpeed = (((85.295 * ZoneExtVar(7, YearCount)) + (69.685 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 4, 4) = RVFCatTraf(2, 4, 4) * (FuelPerKm * (22.1 / 37.6)) * TheStArray(60)
        Else
            RVFFuel(2, 4, 4) = 0
        End If
        'rural minor
        FuelSpeed = 69.685 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 4, 4) = RVFCatTraf(3, 4, 4) * (FuelPerKm * (22.1 / 37.6)) * TheStArray(60)
        'urban
        FuelSpeed = 53.511 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 4, 4) = RVFCatTraf(4, 4, 4) * (FuelPerKm * (22.1 / 37.6)) * TheStArray(60)
        'hydrogen ICE large HGVs
        'motorway
        RVFFuel(1, 4, 9) = RVFCatTraf(1, 4, 9) * 1.398 * TheStArray(61)
        'rural a
        RVFFuel(2, 4, 9) = RVFCatTraf(2, 4, 9) * 1.398 * TheStArray(61)
        'rural minor
        RVFFuel(3, 4, 9) = RVFCatTraf(3, 4, 9) * 1.398 * TheStArray(61)
        'urban
        RVFFuel(4, 4, 9) = RVFCatTraf(4, 4, 9) * 1.398 * TheStArray(61)
        'hydrogen fuel cell large HGVs
        'motorway
        RVFFuel(1, 4, 10) = RVFCatTraf(1, 4, 10) * 1.123 * TheStArray(62)
        'rural a
        RVFFuel(2, 4, 10) = RVFCatTraf(2, 4, 10) * 1.123 * TheStArray(62)
        'rural minor
        RVFFuel(3, 4, 10) = RVFCatTraf(3, 4, 10) * 1.123 * TheStArray(62)
        'urban
        RVFFuel(4, 4, 10) = RVFCatTraf(4, 4, 10) * 1.123 * TheStArray(62)

        'Diesel PSVs
        VClass = "PSVD"
        'motorway
        FuelSpeed = 98.17 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 5, 2) = RVFCatTraf(1, 5, 2) * FuelPerKm * TheStArray(41)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 5) > 0 Then
            FuelSpeed = (((96.561 * ZoneExtVar(7, YearCount)) + (72.42 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 5, 2) = RVFCatTraf(2, 5, 2) * FuelPerKm * TheStArray(41)
        Else
            RVFFuel(2, 5, 2) = 0
        End If
        'rural minor
        FuelSpeed = 72.42 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 5, 2) = RVFCatTraf(3, 5, 2) * FuelPerKm * TheStArray(41)
        'urban
        FuelSpeed = 48.924 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 5, 2) = RVFCatTraf(4, 5, 2) * FuelPerKm * TheStArray(41)
        'Diesel hybrid PSVs
        VClass = "PSVD"
        'motorway
        FuelSpeed = 98.17 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 5, 4) = RVFCatTraf(1, 5, 4) * (FuelPerKm * (18.5 / 17.6)) * TheStArray(52)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 5) > 0 Then
            FuelSpeed = (((96.561 * ZoneExtVar(7, YearCount)) + (72.42 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 5, 4) = RVFCatTraf(2, 5, 4) * (FuelPerKm * (11.9 / 19.6)) * TheStArray(52)
        Else
            RVFFuel(2, 5, 4) = 0
        End If
        'rural minor
        FuelSpeed = 72.42 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 5, 4) = RVFCatTraf(3, 5, 4) * (FuelPerKm * (11.9 / 19.6)) * TheStArray(52)
        'urban
        FuelSpeed = 48.924 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(4, 5, 4) = RVFCatTraf(4, 5, 4) * (FuelPerKm * (11.9 / 19.6)) * TheStArray(52)
        'Plug-in hybrid PSVs
        VClass = "PSVD"
        'motorway
        FuelSpeed = 98.17 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(1, 5, 5) = RVFCatTraf(1, 5, 5) * (FuelPerKm * (11.9 / 19.6)) * TheStArray(53)
        'rural a (note that there are different speeds for dual and single carriagesways)
        If RVCatTraf(2, 5) > 0 Then
            FuelSpeed = (((96.561 * ZoneExtVar(7, YearCount)) + (72.42 * ZoneExtVar(8, YearCount))) / (ZoneExtVar(7, YearCount) + ZoneExtVar(8, YearCount))) * (ZoneSpdNew / BaseSpeed)
            Call VehicleFuelConsumption()
            RVFFuel(2, 5, 5) = RVFCatTraf(2, 5, 5) * (FuelPerKm * (11.9 / 19.6)) * TheStArray(53)
        Else
            RVFFuel(2, 5, 5) = 0
        End If
        'rural minor
        FuelSpeed = 72.42 * (ZoneSpdNew / BaseSpeed)
        Call VehicleFuelConsumption()
        RVFFuel(3, 5, 5) = RVFCatTraf(3, 5, 5) * (FuelPerKm * (11.9 / 19.6)) * TheStArray(53)
        'urban
        RVFFuel(4, 5, 5) = RVFCatTraf(4, 5, 5) * 1.037 * TheStArray(53)
        '***need to alter battery electric PSVs
        'Battery Electric PSVs - electricity consumption figure now from Brand (2010)
        'motorway
        RVFFuel(1, 5, 6) = RVFCatTraf(1, 5, 6) * 1.7 * TheStArray(42)
        'rural a
        RVFFuel(2, 5, 6) = RVFCatTraf(2, 5, 6) * 1.7 * TheStArray(42)
        'rural minor
        RVFFuel(3, 5, 6) = RVFCatTraf(3, 5, 6) * 1.7 * TheStArray(42)
        'urban
        RVFFuel(4, 5, 6) = RVFCatTraf(4, 5, 6) * 1.7 * TheStArray(42)
        'LPG PSVs
        'motorway
        RVFFuel(1, 5, 7) = RVFCatTraf(1, 5, 7) * 0.954 * TheStArray(54)
        'rural a
        RVFFuel(2, 5, 7) = RVFCatTraf(2, 5, 7) * 0.364 * TheStArray(54)
        'rural minor
        RVFFuel(3, 5, 7) = RVFCatTraf(3, 5, 7) * 0.364 * TheStArray(54)
        'urban
        RVFFuel(4, 5, 7) = RVFCatTraf(4, 5, 7) * 0.364 * TheStArray(54)
        'CNG PSVs
        'motorway
        RVFFuel(1, 5, 8) = RVFCatTraf(1, 5, 8) * 3.749 * TheStArray(55)
        'rural a
        RVFFuel(2, 5, 8) = RVFCatTraf(2, 5, 8) * 6.283 * TheStArray(55)
        'rural minor
        RVFFuel(3, 5, 8) = RVFCatTraf(3, 5, 8) * 6.283 * TheStArray(55)
        'urban
        RVFFuel(4, 5, 8) = RVFCatTraf(4, 5, 8) * 6.283 * TheStArray(55)
        'Hydrogen fuel cell PSVs
        'motorway
        RVFFuel(1, 5, 10) = RVFCatTraf(1, 5, 10) * 0.546 * TheStArray(56)
        'rural a
        RVFFuel(2, 5, 10) = RVFCatTraf(2, 5, 10) * 0.546 * TheStArray(56)
        'rural minor
        RVFFuel(3, 5, 10) = RVFCatTraf(3, 5, 10) * 0.546 * TheStArray(56)
        'urban
        RVFFuel(4, 5, 10) = RVFCatTraf(4, 5, 10) * 0.546 * TheStArray(56)

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
        FuelPerKm = (alpha + (beta * FuelSpeed) + (gamma * (FuelSpeed ^ 2)) + (zeta * (FuelSpeed ^ 3))) / FuelSpeed
    End Sub

    Sub RoadZoneOutput()
        'combine output values into output string
        ZoneOutputRow = ZoneID & "," & g_modelRunYear & "," & NewVkm & "," & ZoneSpdNew & "," & PetrolUsed & "," & DieselUsed & "," & ElectricUsed & "," & LPGUsed & "," & CNGUsed & "," & HydrogenUsed

        'write output string to file
        roz.WriteLine(ZoneOutputRow)

    End Sub

    Sub NewBaseValues()
        Dim YearCount As Integer

        YearCount = g_modelRunYear - g_initialYear
        'set base values to equal the values from the current year
        ZonePop = ZoneExtVar(2, YearCount)
        ZoneGVA = ZoneExtVar(3, YearCount)
        ZoneSpeed = ZoneSpdNew
        ZoneCarCost = ZoneExtVar(4, YearCount)
        ZoneLGVCost = ZoneExtVar(42, YearCount)
        ZoneHGV1Cost = ZoneExtVar(43, YearCount)
        ZoneHGV2Cost = ZoneExtVar(44, YearCount)
        ZonePSVCost = ZoneExtVar(45, YearCount)
        BaseVkm = NewVkm
        ZoneLaneKm(1) = ZoneExtVar(6, YearCount)
        ZoneLaneKm(2) = CDbl(ZoneExtVar(7, YearCount)) + CDbl(ZoneExtVar(8, YearCount))
        ZoneLaneKm(3) = ZoneExtVar(9, YearCount)
        ZoneLaneKm(4) = CDbl(ZoneExtVar(10, YearCount)) + CDbl(ZoneExtVar(11, YearCount))
        RoadCatKm(1) = ZoneExtVar(6, YearCount) / 6
        RoadCatKm(2) = (CDbl(ZoneExtVar(7, YearCount)) / 4) + (CDbl(ZoneExtVar(8, YearCount)) / 2)
        RoadCatKm(3) = ZoneExtVar(9, YearCount) / 2
        RoadCatKm(4) = (CDbl(ZoneExtVar(10, YearCount)) / 4) + (CDbl(ZoneExtVar(11, YearCount)) / 2)
        PetrolUsed = 0
        DieselUsed = 0
        ElectricUsed = 0
        LPGUsed = 0
        CNGUsed = 0
        HydrogenUsed = 0

        For x = 1 To 4
            BaseRoadCatTraffic(x) = RoadCatTraffic(x)
            For y = 1 To 5
                BaseRVCatTraf(x, y) = RVCatTraf(x, y)
            Next
        Next
        'add back the suppressed traffic if using smarter choices
        If SmarterChoices = True Then
            BaseRVCatTraf(4, 1) += SuppressedTraffic(4, 1)
            BaseRoadCatTraffic(4) += SuppressedTraffic(4, 1)
            SuppressedTraffic(4, 1) = 0
        End If

        If UrbanFrt = True Then
            For y = 2 To 4
                BaseRVCatTraf(4, y) += SuppressedTraffic(4, y)
                BaseRoadCatTraffic(4) += SuppressedTraffic(4, y)
                SuppressedTraffic(4, y) = 0
            Next
        End If

        If SmartFrt = True Then
            For x = 1 To 3
                For y = 3 To 4
                    BaseRVCatTraf(x, y) += SuppressedTraffic(x, y)
                    BaseRoadCatTraffic(x) += SuppressedTraffic(x, y)
                    SuppressedTraffic(x, y) = 0
                Next
            Next
        End If

    End Sub

End Module
