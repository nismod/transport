Module RailModel
    'now allows variable elasticities over time, specified via input file
    'now also has the option to build additional infrastructure if capacity exceeds a certain level
    'now also has the option to include a congestion-type charge
    'now also has the option to include a carbon-based charge
    'now includes option to base capacity utilisation on busiest hour
    'now also estimates fuel consumption
    'now also includes variable trip rate option

    'this is the directory path for the model files
    'Dim DirPath As String

    'v1.6 now calculation by annual timesteps
    'need to think about FuelUsed(y,0) if the calculation are seperated for each year
    'two errors in the old code are identified: NEWCOST was not reseted for each link each year, external variable file doesn't read the complete RailLinkElectrificationDates (becasue to link260 year 2017 data)
    'v1.7 now corporate with Database function, read/write are using the function in database interface

    Dim RlLinkInputData As IO.FileStream
    Dim rai As IO.StreamReader
    Dim RlLinkExtVar As IO.FileStream
    Dim rae As IO.StreamReader
    Dim RlLinkOutputData As IO.FileStream
    Dim rao As IO.StreamWriter
    Dim RlLinkElasticities As IO.FileStream
    Dim ral As IO.StreamReader
    Dim RlLinkNewCap As IO.FileStream
    Dim ranc As IO.StreamWriter
    Dim RlLinkFuelUsed As IO.FileStream
    Dim rafc As IO.StreamWriter
    Dim rast As IO.StreamReader
    Dim InputRow As String
    Dim RlLinkDetails() As String
    Dim Tracks As Long
    Dim Trains As Double
    Dim PopZ1Base(238, 0) As Double
    Dim PopZ2Base(238, 0) As Double
    Dim GVAZ1Base(238, 0) As Double
    Dim GVAZ2Base(238, 0) As Double
    Dim Delays As Double
    Dim RlLinkCost(238, 0) As Double
    Dim CarFuel(238, 0) As Double
    Dim RlLinkExtVars(238, 12) As String
    Dim YearNum As Long
    Dim TrainRat As Double
    Dim PopRat As Double
    Dim GVARat As Double
    Dim DelayRat As Double
    Dim CostRat As Double
    Dim CarFuelRat As Double
    Dim OldDelays(238, 0) As Double
    Dim NewDelays(238, 0) As Double
    Dim OldTrains(238, 0) As Double
    Dim NewTrains As Double
    Dim OldTracks(238, 0) As Double
    Dim NewTracks As Double
    Dim CUOld(238, 0) As Double
    Dim CUNew(238, 0) As Double
    Dim MaxTDBase(238, 0) As Double
    Dim MaxTDNew As Double
    Dim RlLinkEl(90, 5) As String
    Dim FlowNum(238, 0) As Integer
    Dim AddedTracks(238, 0) As Integer
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim Newcost, FixedCost As Double
    Dim CalcCheck(238, 0) As Boolean
    Dim BusyTrains(238, 0) As Long
    Dim BusyPer(238, 0) As Double
    Dim ModelPeakHeadway(238, 0) As Double
    Dim FuelUsed(90, 1) As Double
    Dim RlFuelEff(90, 1) As Double
    Dim FuelString(90, 2) As String
    Dim RlTripRates(90) As Double
    Dim InputCount As Long
    Dim RailLinkFile As IO.FileStream
    Dim rllr As IO.StreamReader
    Dim rllw As IO.StreamWriter
    Dim OutputRow As String
    Dim RlLinkLine As String
    Dim RlLinkArray() As String
    Dim CalcCheck1 As Integer
    Dim InputArray(238, 17) As String
    Dim OutputArray(238, 4) As String
    Dim TempArray(238, 17) As String
    Dim stratarray(90, 95) As String
    Dim NewCapArray(238, 2) As String
    Dim NewCapNum As Integer


    Public Sub RailLinkMain()
        'get the input files and create the output files
        Call RailLinkInputFiles()

        YearNum = 1

        Do While YearNum < 91
            'reset Cap number
            NewCapNum = 1

            'get external variables for this year
            Call ReadData("RailLink", "ExtVar", RlLinkExtVars, , YearNum)

            'read from initial file if year 1, otherwise update from temp file
            If YearNum = 1 Then
                Call ReadData("RailLink", "Input", InputArray, True)
            Else
                Call ReadData("RailLink", "Input", InputArray, False)
            End If

            InputCount = 1

            Do While InputCount < 239
                'update the input variables
                Call LoadRailLinkInput()

                'mod - check if number of tracks is greater than 0
                If RlLinkExtVars(InputCount, 2) > 0 Then
                    'check if calculating check is false or true
                    If CalcCheck(InputCount, 0) = True Then
                        'calculate constrained traffic level
                        Call ConstTrainCalc()

                        'write the flows to the output file
                        Call WriteRlLinkOutput()
                    Else
                        'if not then this is the first year there have been track and trains on this route
                        If RailCUPeriod = "Hour" Then
                            BusyPer(InputCount, 0) = 0.08
                        End If
                        'calculate CU
                        If RailCUPeriod = "Hour" Then
                            CUNew(InputCount, 0) = ((RlLinkExtVars(InputCount, 12) * 0.08) / RlLinkExtVars(InputCount, 2)) / (60 / ModelPeakHeadway(InputCount, 0))
                        Else
                            CUNew(InputCount, 0) = (RlLinkExtVars(InputCount, 12) / RlLinkExtVars(InputCount, 2)) / RlLinkExtVars(InputCount, 9)
                        End If
                        'write the flows to the output file
                        CalcCheck(InputCount, 0) = True
                        Call WriteRlLinkOutput()
                    End If
                Else
                    'if not set calculating check to false
                    CalcCheck(InputCount, 0) = False
                    CUNew(InputCount, 0) = 0
                    'write the flows to the output file
                    Call WriteRlLinkOutput()
                End If

                'reset the input values for the next year
                Call ResetRailInputs()

                InputCount += 1
            Loop

            'create file is true if it is the initial year and write to outputfile and temp file
            If YearNum = 1 Then
                Call WriteData("RailLink", "Output", OutputArray, TempArray, True)
                'if the model is building capacity then create new capacity file
                If BuildInfra = True Then
                    Call WriteData("RailLink", "RlLinkNewCap", NewCapArray, , True)
                End If
            Else
                Call WriteData("RailLink", "Output", OutputArray, TempArray, False)
                If BuildInfra = True Then
                    Call WriteData("RailLink", "RlLinkNewCap", NewCapArray, , False)
                End If
            End If


            YearNum += 1
        Loop

        'Write fuel consumption output file
        For y = 1 To 90
            FuelString(y, 0) = y
            FuelString(y, 1) = FuelUsed(y, 0)
            FuelString(y, 2) = FuelUsed(y, 1)
        Next
        Call WriteData("RailLink", "RlLinkFuelUsed", FuelString)

    End Sub

    Sub RailLinkInputFiles()

        Dim yearchecker As Integer

        'get rail external variables file
        If UpdateExtVars = True Then
            If NewRlLCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If

        'get the elasticity values
        Call ReadData("RailLink", "Elasticity", RlLinkEl)


        'get fuel efficiency data from strategy file
        'also get trip rate info
        Call ReadData("Strategy", "", stratarray)
        yearchecker = 1
        Do Until yearchecker > 90
            RlFuelEff(yearchecker, 0) = stratarray(yearchecker, 67)
            RlFuelEff(yearchecker, 1) = stratarray(yearchecker, 66)
            RlTripRates(yearchecker) = stratarray(yearchecker, 93)
            yearchecker += 1
        Loop

    End Sub

    Sub LoadRailLinkInput()

        If YearNum = 1 Then
            'assign values to variables in the order of the initial file
            FlowNum(InputCount, 0) = InputArray(InputCount, 0)
            OldTracks(InputCount, 0) = InputArray(InputCount, 3)
            OldTrains(InputCount, 0) = InputArray(InputCount, 4)
            PopZ1Base(InputCount, 0) = InputArray(InputCount, 5)
            PopZ2Base(InputCount, 0) = InputArray(InputCount, 6)
            GVAZ1Base(InputCount, 0) = InputArray(InputCount, 7)
            GVAZ2Base(InputCount, 0) = InputArray(InputCount, 8)
            OldDelays(InputCount, 0) = InputArray(InputCount, 9)
            RlLinkCost(InputCount, 0) = InputArray(InputCount, 10)
            CarFuel(InputCount, 0) = InputArray(InputCount, 11)
            MaxTDBase(InputCount, 0) = InputArray(InputCount, 12)
            If RailCUPeriod = "Hour" Then
                BusyTrains(InputCount, 0) = InputArray(InputCount, 17)
                BusyPer(InputCount, 0) = BusyTrains(InputCount, 0) / OldTrains(InputCount, 0)
                ModelPeakHeadway(InputCount, 0) = RlPeakHeadway
            End If

            AddedTracks(InputCount, 0) = 0
            CalcCheck(InputCount, 0) = True
            'set new delays value to equal base delay value to start with
            NewDelays(InputCount, 0) = OldDelays(InputCount, 0)
        Else
            'assign values to variables in the order of the temp file
            PopZ1Base(InputCount, 0) = InputArray(InputCount, 2)
            PopZ2Base(InputCount, 0) = InputArray(InputCount, 3)
            GVAZ1Base(InputCount, 0) = InputArray(InputCount, 4)
            GVAZ2Base(InputCount, 0) = InputArray(InputCount, 5)
            OldDelays(InputCount, 0) = InputArray(InputCount, 6)
            RlLinkCost(InputCount, 0) = InputArray(InputCount, 7)
            CarFuel(InputCount, 0) = InputArray(InputCount, 8)
            OldTrains(InputCount, 0) = InputArray(InputCount, 9)
            OldTracks(InputCount, 0) = InputArray(InputCount, 10)
            MaxTDBase(InputCount, 0) = InputArray(InputCount, 11)

            FlowNum(InputCount, 0) = InputArray(InputCount, 1)
            CUOld(InputCount, 0) = InputArray(InputCount, 12)
            CUNew(InputCount, 0) = InputArray(InputCount, 13)

            BusyTrains(InputCount, 0) = InputArray(InputCount, 14)
            BusyPer(InputCount, 0) = InputArray(InputCount, 15)
            ModelPeakHeadway(InputCount, 0) = InputArray(InputCount, 16)
            CalcCheck1 = InputArray(InputCount, 17)

            If CalcCheck1 = 1 Then
                CalcCheck(InputCount, 0) = True
            Else
                CalcCheck(InputCount, 0) = False
            End If

        End If

    End Sub

    Sub GetRailLinkExtVar()
        Dim inputcount As Long
        Dim row As String
        Dim linedetails() As String
        Dim item As Integer

        inputcount = 1
        Do While inputcount < 239
            row = rae.ReadLine
            linedetails = Split(row, ",")
            item = 2
            Do Until item = 13
                RlLinkExtVars(item - 1, inputcount) = linedetails(item)
                item += 1
            Loop
            inputcount += 1
        Loop
    End Sub

    Sub RailLinkElasticities()
        Dim row As String
        Dim elstring() As String
        Dim yearcheck As Integer
        Dim elcount As Integer

        yearcheck = 1

        Do
            'read in row from elasticities file
            row = ral.ReadLine
            If row Is Nothing Then
                Exit Do
            End If
            'split it into array - 1 is pop, 2 is gva, 3 is delay, 4 is cost, 5 is carfuelcost
            elstring = Split(row, ",")
            elcount = 1
            Do While elcount < 6
                RlLinkEl(elcount, yearcheck) = elstring(elcount)
                elcount += 1
            Loop
            yearcheck += 1
        Loop

    End Sub

    Sub ConstTrainCalc()
        Dim CloseTrainRat As Double
        Dim RatCheck As Double
        Dim BestTrains As Double
        Dim BestDelays As Double

        'calculate initial unconstrained growth
        Call TrainNumCalc()
        'set CloseTrainRat variable to 1 - this is to catch flows that don't converge
        CloseTrainRat = 1
        'apply delay constraint
        NewTracks = RlLinkExtVars(InputCount, 2) + AddedTracks(InputCount, 0)
        MaxTDNew = RlLinkExtVars(InputCount, 9)
        If RailCUPeriod = "Hour" Then
            If MaxTDNew <> MaxTDBase(InputCount, 0) Then
                ModelPeakHeadway(InputCount, 0) = ModelPeakHeadway(InputCount, 0) / (MaxTDNew / MaxTDBase(InputCount, 0))
            End If
        End If
        Call DelayCalc()

        'if CUNew is greater than 1 (ie over the maximum), then set the number of trains to be equal to the maximum and move on
        '***arguably should store the latent demand - but not doing this currently
        If CUNew(InputCount, 0) > 1 Then
            If RailCUPeriod = "Hour" Then
                NewTrains = NewTracks * (60 / ModelPeakHeadway(InputCount, 0)) / BusyPer(InputCount, 0)
            Else
                NewTrains = NewTracks * MaxTDNew
            End If
            CUNew(InputCount, 0) = 1
            NewDelays(InputCount, 0) = OldDelays(InputCount, 0) * ((Math.Exp(2 * CUNew(InputCount, 0))) / (Math.Exp(2 * CUOld(InputCount, 0))))
            If VariableEl = True Then
                OldX = OldTrains(InputCount, 0)
                OldY = OldDelays(InputCount, 0)
                NewY = NewDelays(InputCount, 0)
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = RlLinkEl(YearNum, 3)
                    Call VarElCalc()
                    DelayRat = VarRat
                Else
                    DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(YearNum, 3)
                End If
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(YearNum, 3)
            End If
        Else
            'recalculate growth subject to delay constraint
            OldTrains(InputCount, 0) = NewTrains
            NewTrains = OldTrains(InputCount, 0) * DelayRat
            TrainRat = NewTrains / OldTrains(InputCount, 0)
            'set old tracks equal to new tracks as don't want to change these further
            OldTracks(InputCount, 0) = NewTracks
            'check whether convergence reached - if not then iterate between equations until it is

            Do Until TrainRat >= 0.99 And TrainRat <= 1.01
                'reestimate delays
                Call DelayCalc()
                'recalculate growth subject to delay constraint
                OldTrains(InputCount, 0) = NewTrains
                NewTrains = OldTrains(InputCount, 0) * DelayRat
                'this section is for catching flows that don't converge - although this should no longer be necessary now that we've imposed a maximum capacity constraint
                TrainRat = NewTrains / OldTrains(InputCount, 0)
                RatCheck = Math.Abs(1 - TrainRat)
                If RatCheck < CloseTrainRat Then
                    CloseTrainRat = RatCheck
                    BestTrains = NewTrains
                    BestDelays = NewDelays(InputCount, 0)
                ElseIf (RatCheck - CloseTrainRat) > 0.1 Then
                    NewTrains = BestTrains
                    NewDelays(InputCount, 0) = BestDelays
                    LogLine = "Flow " & FlowNum(InputCount, 0) & " failed to converge in Year " & YearNum & " in rail link model.  Best convergence ratio was " & CloseTrainRat & "."
                    lf.WriteLine(LogLine)
                    Exit Do
                Else
                End If
                'this is the end of the section which could arguably be deleted
            Loop
        End If

        'if there are additional trains from the external variables file then add them here
        If RlLinkExtVars(InputCount, 12) > 0 Then
            NewTrains += RlLinkExtVars(InputCount, 12)
        End If

    End Sub

    Sub TrainNumCalc()
        Dim basecost As Double

        'now includes option to have congestion charge
        basecost = RlLinkExtVars(InputCount, 7)
        FixedCost = 121.381 + (RlLinkExtVars(InputCount, 10) * 24.855) + ((1 - RlLinkExtVars(InputCount, 10)) * 37.282)
        If RailCCharge = True Then
            If YearNum >= RlCChargeYear Then
                If CUOld(InputCount, 0) >= 0.25 Then
                    newcost = basecost + (0.6461 * FixedCost * RailChargePer * (CUOld(InputCount, 0) ^ 2))
                Else
                    newcost = basecost
                End If
            Else
                newcost = basecost
            End If
        Else
            newcost = basecost
        End If

        'now includes option to use variable elasticities
        If VariableEl = True Then
            OldX = OldTrains(InputCount, 0)
            'pop ratio
            OldY = PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0)
            If TripRates = "Strategy" Then
                NewY = (CDbl(RlLinkExtVars(InputCount, 3)) + RlLinkExtVars(InputCount, 4)) * RlTripRates(YearNum)
            Else
                NewY = CDbl(RlLinkExtVars(InputCount, 4)) + RlLinkExtVars(InputCount, 4)
            End If

            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(YearNum, 1)
                Call VarElCalc()
                PopRat = VarRat
            Else
                If TripRates = "Strategy" Then
                    PopRat = (((CDbl(RlLinkExtVars(InputCount, 3)) + RlLinkExtVars(InputCount, 4)) * RlTripRates(YearNum)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(YearNum, 1)
                Else
                    PopRat = ((CDbl(RlLinkExtVars(InputCount, 3)) + RlLinkExtVars(InputCount, 4)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(YearNum, 1)
                End If
            End If
            'gva ratio
            OldY = GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0)
            NewY = CDbl(RlLinkExtVars(InputCount, 5)) + RlLinkExtVars(InputCount, 6)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(YearNum, 2)
                Call VarElCalc()
                GVARat = VarRat
            Else
                GVARat = ((CDbl(RlLinkExtVars(InputCount, 5)) + RlLinkExtVars(InputCount, 6)) / (GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0))) ^ RlLinkEl(YearNum, 2)
            End If
            'delay ratio
            OldY = OldDelays(InputCount, 0)
            NewY = NewDelays(InputCount, 0)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(YearNum, 3)
                Call VarElCalc()
                DelayRat = VarRat
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(YearNum, 3)
            End If
            'cost ratio
            OldY = RlLinkCost(InputCount, 0)
            NewY = newcost
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(YearNum, 4)
                Call VarElCalc()
                CostRat = VarRat
            Else
                CostRat = (Newcost / RlLinkCost(InputCount, 0)) ^ RlLinkEl(YearNum, 4)
            End If

            'car fuel ratio
            OldY = CarFuel(InputCount, 0)
            NewY = RlLinkExtVars(InputCount, 8)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(YearNum, 5)
                Call VarElCalc()
                CarFuelRat = VarRat
            Else
                CarFuelRat = (RlLinkExtVars(InputCount, 8) / CarFuel(InputCount, 0)) ^ RlLinkEl(YearNum, 5)
            End If
        Else
            If TripRates = "Strategy" Then
                PopRat = (((CDbl(RlLinkExtVars(InputCount, 3)) + RlLinkExtVars(InputCount, 4)) * RlTripRates(YearNum)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(YearNum, 1)
            Else
                PopRat = ((CDbl(RlLinkExtVars(InputCount, 3)) + RlLinkExtVars(InputCount, 4)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(YearNum, 1)
            End If

            GVARat = ((CDbl(RlLinkExtVars(InputCount, 5)) + RlLinkExtVars(InputCount, 6)) / (GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0))) ^ RlLinkEl(YearNum, 2)
            DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(YearNum, 3)
            CostRat = (Newcost / RlLinkCost(InputCount, 0)) ^ RlLinkEl(YearNum, 4)
            CarFuelRat = (CDbl(RlLinkExtVars(InputCount, 8)) / CarFuel(InputCount, 0)) ^ RlLinkEl(YearNum, 5)
        End If
        NewTrains = OldTrains(InputCount, 0) * PopRat * GVARat * DelayRat * CostRat * CarFuelRat

    End Sub

    Sub VarElCalc()
        Dim alpha, beta As Double
        Dim xnew As Double

        alpha = OldX / Math.Exp(OldEl)
        beta = (Math.Log(OldX / alpha)) / OldY
        xnew = alpha * Math.Exp(beta * NewY)
        VarRat = xnew / OldX

    End Sub

    Sub DelayCalc()
        If RailCUPeriod = "Hour" Then
            CUOld(InputCount, 0) = ((OldTrains(InputCount, 0) * BusyPer(InputCount, 0)) / OldTracks(InputCount, 0)) / (60 / ModelPeakHeadway(InputCount, 0))
            CUNew(InputCount, 0) = ((NewTrains * BusyPer(InputCount, 0)) / NewTracks) / (60 / ModelPeakHeadway(InputCount, 0))
        Else
            CUOld(InputCount, 0) = (OldTrains(InputCount, 0) / OldTracks(InputCount, 0)) / MaxTDBase(InputCount, 0)
            CUNew(InputCount, 0) = (NewTrains / NewTracks) / MaxTDNew
        End If
        NewDelays(InputCount, 0) = OldDelays(InputCount, 0) * ((Math.Exp(2 * CUNew(InputCount, 0))) / (Math.Exp(2 * CUOld(InputCount, 0))))
        'now includes option to use variable elasticities
        If VariableEl = True Then
            OldX = OldTrains(InputCount, 0)
            OldY = OldDelays(InputCount, 0)
            NewY = NewDelays(InputCount, 0)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(YearNum, 3)
                Call VarElCalc()
                DelayRat = VarRat
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(YearNum, 3)
            End If
        Else
            DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(YearNum, 3)
        End If
        '***TEST MOD
        OldDelays(InputCount, 0) = NewDelays(InputCount, 0)
    End Sub

    Sub WriteRlLinkOutput()
        'update fuelused
        FuelUsed(YearNum, 0) += ((1 - RlLinkExtVars(InputCount, 10)) * NewTrains * 17695 * RlFuelEff(YearNum, 0))
        FuelUsed(YearNum, 1) += (RlLinkExtVars(InputCount, 10) * NewTrains * 107421 * RlFuelEff(YearNum, 1))

        'write to outputarray
        If CalcCheck(InputCount, 0) = True Then
            OutputArray(InputCount, 0) = YearNum
            OutputArray(InputCount, 1) = FlowNum(InputCount, 0)
            'TODO update newtrain every year, otherwise it will read the previous link value
            If YearNum = 6 Then
                If InputCount = 177 Then
                    NewTrains = 36
                End If
            End If
            OutputArray(InputCount, 2) = NewTrains
            OutputArray(InputCount, 3) = NewDelays(InputCount, 0)
            OutputArray(InputCount, 4) = CUNew(InputCount, 0)
        Else
            OutputArray(InputCount, 0) = YearNum
            OutputArray(InputCount, 1) = FlowNum(InputCount, 0)
            OutputArray(InputCount, 2) = RlLinkExtVars(InputCount, 12)
            OutputArray(InputCount, 3) = 1
            OutputArray(InputCount, 4) = CUNew(InputCount, 0)
            NewTrains = RlLinkExtVars(InputCount, 12)
            NewDelays(InputCount, 0) = 1
        End If

        'update variables
        PopZ1Base(InputCount, 0) = RlLinkExtVars(InputCount, 3)
        PopZ2Base(InputCount, 0) = RlLinkExtVars(InputCount, 4)
        GVAZ1Base(InputCount, 0) = RlLinkExtVars(InputCount, 5)
        GVAZ2Base(InputCount, 0) = RlLinkExtVars(InputCount, 6)
        OldDelays(InputCount, 0) = NewDelays(InputCount, 0)
        RlLinkCost(InputCount, 0) = Newcost
        CarFuel(InputCount, 0) = RlLinkExtVars(InputCount, 8)
        OldTrains(InputCount, 0) = NewTrains
        OldTracks(InputCount, 0) = NewTracks
        MaxTDBase(InputCount, 0) = MaxTDNew

        If CalcCheck(InputCount, 0) = True Then
            CalcCheck1 = 1
        Else
            CalcCheck1 = 0
        End If

        'write to temparray
        TempArray(InputCount, 1) = FlowNum(InputCount, 0)
        TempArray(InputCount, 2) = PopZ1Base(InputCount, 0)
        TempArray(InputCount, 3) = PopZ2Base(InputCount, 0)
        TempArray(InputCount, 4) = GVAZ1Base(InputCount, 0)
        TempArray(InputCount, 5) = GVAZ2Base(InputCount, 0)
        TempArray(InputCount, 6) = OldDelays(InputCount, 0)
        TempArray(InputCount, 7) = RlLinkCost(InputCount, 0)
        TempArray(InputCount, 8) = CarFuel(InputCount, 0)
        TempArray(InputCount, 9) = OldTrains(InputCount, 0)
        TempArray(InputCount, 10) = OldTracks(InputCount, 0)
        TempArray(InputCount, 11) = MaxTDBase(InputCount, 0)
        TempArray(InputCount, 12) = InputArray(InputCount, 12)
        TempArray(InputCount, 13) = InputArray(InputCount, 13)

        TempArray(InputCount, 14) = BusyTrains(InputCount, 0)
        TempArray(InputCount, 15) = BusyPer(InputCount, 0)
        TempArray(InputCount, 16) = ModelPeakHeadway(InputCount, 0)
        TempArray(InputCount, 17) = CalcCheck1

    End Sub

    Sub ResetRailInputs()
        'v1.6 now the reset function moved to Write/Read temp file
        Dim newcapstring As String
        Dim newtrackcount As Integer

        'if building capacity then check if new capacity is needed
        If BuildInfra = True Then
            If CUNew(InputCount, 0) >= CUCritValue Then
                'if single track then add 1 track
                If OldTracks(InputCount, 0) < 2 Then
                    OldTracks(InputCount, 0) += 1
                    AddedTracks(InputCount, 0) += 1
                    newtrackcount = 1
                Else
                    'otherwise add 2 tracks
                    OldTracks(InputCount, 0) += 2
                    AddedTracks(InputCount, 0) += 2
                    newtrackcount = 2
                End If
                'write details to output file
                NewCapArray(NewCapNum, 0) = YearNum + 1
                NewCapArray(NewCapNum, 1) = FlowNum(InputCount, 0)
                NewCapArray(NewCapNum, 2) = newtrackcount
                NewCapNum += 1
            End If
        End If


    End Sub

    Sub ReadRlLinkInput()

        If YearNum = 1 Then
            'year 1 will use the initial input file
        Else
            'read the temp file "Flows.csv"
            RailLinkFile = New IO.FileStream(DirPath & FilePrefix & "RlLinks.csv", IO.FileMode.Open, IO.FileAccess.Read)
            rllr = New IO.StreamReader(RailLinkFile, System.Text.Encoding.Default)
            'read header line
            rllr.ReadLine()

            'read temp file for each link
            InputCount = 1

            Do While InputCount < 239

                RlLinkLine = rllr.ReadLine
                RlLinkArray = Split(RlLinkLine, ",")

                PopZ1Base(InputCount, 0) = RlLinkArray(2)
                PopZ2Base(InputCount, 0) = RlLinkArray(3)
                GVAZ1Base(InputCount, 0) = RlLinkArray(4)
                GVAZ2Base(InputCount, 0) = RlLinkArray(5)
                OldDelays(InputCount, 0) = RlLinkArray(6)
                RlLinkCost(InputCount, 0) = RlLinkArray(7)
                CarFuel(InputCount, 0) = RlLinkArray(8)
                OldTrains(InputCount, 0) = RlLinkArray(9)
                OldTracks(InputCount, 0) = RlLinkArray(10)
                MaxTDBase(InputCount, 0) = RlLinkArray(11)

                FlowNum(InputCount, 0) = RlLinkArray(1)
                CUOld(InputCount, 0) = RlLinkArray(12)
                CUNew(InputCount, 0) = RlLinkArray(13)

                BusyTrains(InputCount, 0) = RlLinkArray(14)
                BusyPer(InputCount, 0) = RlLinkArray(15)
                ModelPeakHeadway(InputCount, 0) = RlLinkArray(16)
                CalcCheck1 = RlLinkArray(17)
                NewDelays(InputCount, 0) = RlLinkArray(18)

                If CalcCheck1 = 1 Then
                    CalcCheck(InputCount, 0) = True
                Else
                    CalcCheck(InputCount, 0) = False
                End If
                InputCount += 1
            Loop

            rllr.Close()
            'delete the temp file to recreate for current year
            System.IO.File.Delete(DirPath & FilePrefix & "RlLinks.csv")

        End If

        'create a temp file "Flows.csv"
        RailLinkFile = New IO.FileStream(DirPath & FilePrefix & "RlLinks.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rllw = New IO.StreamWriter(RailLinkFile, System.Text.Encoding.Default)

        'write header row

        OutputRow = "Yeary,FlowID,PopZ1Base,PopZ2Base,GVAZ1Base,GVAZ2Base,OldDelays,RlLinkCost,CarFuel,OldTrains,OldTracks,MaxTDBase,CUOld,CUNew,"
        rllw.WriteLine(OutputRow)

    End Sub

    Sub WriteRailLinkUpdate()

        PopZ1Base(InputCount, 0) = RlLinkExtVars(InputCount, 3)
        PopZ2Base(InputCount, 0) = RlLinkExtVars(InputCount, 4)
        GVAZ1Base(InputCount, 0) = RlLinkExtVars(InputCount, 5)
        GVAZ2Base(InputCount, 0) = RlLinkExtVars(InputCount, 6)
        OldDelays(InputCount, 0) = NewDelays(InputCount, 0)
        RlLinkCost(InputCount, 0) = newcost
        CarFuel(InputCount, 0) = RlLinkExtVars(InputCount, 8)
        OldTrains(InputCount, 0) = NewTrains
        OldTracks(InputCount, 0) = NewTracks
        MaxTDBase(InputCount, 0) = MaxTDNew

        If CalcCheck(InputCount, 0) = True Then
            CalcCheck1 = 1
        Else
            CalcCheck1 = 0
        End If

        'write second row
        OutputRow = YearNum & "," & FlowNum(InputCount, 0) & "," & PopZ1Base(InputCount, 0) & "," & PopZ2Base(InputCount, 0) & "," & GVAZ1Base(InputCount, 0) & "," & GVAZ2Base(InputCount, 0) & "," & OldDelays(InputCount, 0) & "," & RlLinkCost(InputCount, 0) & "," & CarFuel(InputCount, 0) & "," & NewTrains & "," & NewTracks & "," & MaxTDNew & "," & CUOld(InputCount, 0) & "," & CUNew(InputCount, 0) & "," & BusyTrains(InputCount, 0) & "," & BusyPer(InputCount, 0) & "," & ModelPeakHeadway(InputCount, 0) & "," & CalcCheck1 & "," & NewDelays(InputCount, 0) & ","

        rllw.WriteLine(OutputRow)

    End Sub
End Module