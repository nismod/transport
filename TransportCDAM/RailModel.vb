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
    Dim RlLinkExtVars(11, 238) As Double
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
    Dim RlLinkEl(5, 90) As Double
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
    Dim FuelString As String
    Dim RlTripRates(90) As Double
    Dim InputCount As Long
    Dim RailLinkFile As IO.FileStream
    Dim rllr As IO.StreamReader
    Dim rllw As IO.StreamWriter
    Dim OutputRow As String
    Dim RlLinkLine As String
    Dim RlLinkArray() As String
    Dim CalcCheck1 As Integer

    Public Sub RailLinkMain()
        'get the input files and create the output files
        Call RailLinkInputFiles()

        'get the elasticity values
        Call RailLinkElasticities()

        YearNum = 1

        Do While YearNum < 91

            'get external variables for this year
            Call GetRailLinkExtVar()

            'read from temp file if not year 1
            Call ReadRlLinkInput()

            InputCount = 1

            Do While InputCount < 239
                'update the input variables
                Call LoadRailLinkInput()

                'mod - check if number of tracks is greater than 0
                If RlLinkExtVars(1, InputCount) > 0 Then
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
                            CUNew(InputCount, 0) = ((RlLinkExtVars(11, InputCount) * 0.08) / RlLinkExtVars(1, InputCount)) / (60 / ModelPeakHeadway(InputCount, 0))
                        Else
                            CUNew(InputCount, 0) = (RlLinkExtVars(11, InputCount) / RlLinkExtVars(1, InputCount)) / RlLinkExtVars(8, InputCount)
                        End If
                        'write the flows to the output file
                        Call WriteRlLinkOutput()
                        CalcCheck(InputCount, 0) = True
                    End If
                Else
                    'if not set calculating check to false
                    CalcCheck(InputCount, 0) = False
                    CUNew(InputCount, 0) = 0
                    'write the flows to the output file
                    Call WriteRlLinkOutput()
                End If

                'write temp file for next year
                Call WriteRailLinkUpdate()

                'reset the input values for the next year
                Call ResetRailInputs()

                InputCount += 1
            Loop

            'close the temp file
            rllw.Close()

            YearNum += 1
        Loop
        'Write fuel consumption output file
        For y = 1 To 90
            FuelString = y & "," & FuelUsed(y, 0) & "," & FuelUsed(y, 1)
            rafc.WriteLine(FuelString)
        Next

        'close input and output files
        rai.Close()
        rae.Close()
        rao.Close()
        ral.Close()
        rafc.Close()
        If BuildInfra = True Then
            ranc.Close()
        End If
    End Sub

    Sub RailLinkInputFiles()

        Dim stratstring() As String
        Dim yearchecker As Integer

        'set dir path
        'DirPath = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Inputs\"

        'get rail link input data
        RlLinkInputData = New IO.FileStream(DirPath & "RailLinkInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rai = New IO.StreamReader(RlLinkInputData, System.Text.Encoding.Default)
        'read the header row
        InputRow = rai.ReadLine

        'get rail external variables file
        If UpdateExtVars = True Then
            If NewRlLCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If
        RlLinkExtVar = New IO.FileStream(DirPath & EVFilePrefix & "RailLinkExtVar" & EVFileSuffix & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        rae = New IO.StreamReader(RlLinkExtVar, System.Text.Encoding.Default)
        'read the header row
        InputRow = rae.ReadLine

        'create rail link output file
        RlLinkOutputData = New IO.FileStream(DirPath & FilePrefix & "RailLinkOutputData.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rao = New IO.StreamWriter(RlLinkOutputData, System.Text.Encoding.Default)
        'write the header row
        rao.WriteLine("Yeary,FlowID,Trainsy,Delaysy,CUy")

        'get rail link elasticities file
        RlLinkElasticities = New IO.FileStream(DirPath & "Elasticity Files\TR" & Strategy & "\RailLinkElasticities.csv", IO.FileMode.Open, IO.FileAccess.Read)
        ral = New IO.StreamReader(RlLinkElasticities, System.Text.Encoding.Default)
        'read the header row
        InputRow = ral.ReadLine

        'if the model is building capacity then create new capacity file
        If BuildInfra = True Then
            RlLinkNewCap = New IO.FileStream(DirPath & FilePrefix & "RailLinkNewCap.csv", IO.FileMode.Create, IO.FileAccess.Write)
            ranc = New IO.StreamWriter(RlLinkNewCap, System.Text.Encoding.Default)
            'write header row
            ranc.WriteLine("Yeary,FlowID,TracksAdded")
        End If

        RlLinkFuelUsed = New IO.FileStream(DirPath & FilePrefix & "RailLinkFuelConsumption.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rafc = New IO.StreamWriter(RlLinkFuelUsed, System.Text.Encoding.Default)
        'write the header row
        rafc.WriteLine("Yeary,Diesely,Electricy")

        'get fuel efficiency data from strategy file
        'also get trip rate info
        StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        rast = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
        'read header row
        rast.ReadLine()
        InputRow = rast.ReadLine
        yearchecker = 1
        Do Until InputRow Is Nothing
            stratstring = Split(InputRow, ",")
            RlFuelEff(yearchecker, 0) = stratstring(67)
            RlFuelEff(yearchecker, 1) = stratstring(66)
            RlTripRates(yearchecker) = stratstring(93)
            InputRow = rast.ReadLine
            yearchecker += 1
        Loop
        rast.Close()

    End Sub

    Sub LoadRailLinkInput()

        If YearNum = 1 Then
            InputRow = rai.ReadLine

            RlLinkDetails = Split(InputRow, ",")
            'assign values to variables
            FlowNum(InputCount, 0) = RlLinkDetails(0)
            OldTracks(InputCount, 0) = RlLinkDetails(3)
            OldTrains(InputCount, 0) = RlLinkDetails(4)
            PopZ1Base(InputCount, 0) = RlLinkDetails(5)
            PopZ2Base(InputCount, 0) = RlLinkDetails(6)
            GVAZ1Base(InputCount, 0) = RlLinkDetails(7)
            GVAZ2Base(InputCount, 0) = RlLinkDetails(8)
            OldDelays(InputCount, 0) = RlLinkDetails(9)
            RlLinkCost(InputCount, 0) = RlLinkDetails(10)
            CarFuel(InputCount, 0) = RlLinkDetails(11)
            MaxTDBase(InputCount, 0) = RlLinkDetails(12)
            If RailCUPeriod = "Hour" Then
                BusyTrains(InputCount, 0) = RlLinkDetails(17)
                BusyPer(InputCount, 0) = BusyTrains(InputCount, 0) / OldTrains(InputCount, 0)
                ModelPeakHeadway(InputCount, 0) = RlPeakHeadway
            End If

            AddedTracks(InputCount, 0) = 0
            CalcCheck(InputCount, 0) = True
            'set new delays value to equal base delay value to start with
            NewDelays(InputCount, 0) = OldDelays(InputCount, 0)
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
        NewTracks = RlLinkExtVars(1, InputCount) + AddedTracks(InputCount, 0)
        MaxTDNew = RlLinkExtVars(8, InputCount)
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
                    OldEl = RlLinkEl(3, YearNum)
                    Call VarElCalc()
                    DelayRat = VarRat
                Else
                    DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(3, YearNum)
                End If
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(3, YearNum)
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
        If RlLinkExtVars(11, InputCount) > 0 Then
            NewTrains += RlLinkExtVars(11, InputCount)
        End If

    End Sub

    Sub TrainNumCalc()
        Dim basecost As Double

        'now includes option to have congestion charge
        basecost = RlLinkExtVars(6, InputCount)
        FixedCost = 121.381 + (RlLinkExtVars(9, InputCount) * 24.855) + ((1 - RlLinkExtVars(9, InputCount)) * 37.282)
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
                NewY = (RlLinkExtVars(2, InputCount) + RlLinkExtVars(3, InputCount)) * RlTripRates(YearNum)
            Else
                NewY = RlLinkExtVars(2, InputCount) + RlLinkExtVars(3, InputCount)
            End If

            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(1, YearNum)
                Call VarElCalc()
                PopRat = VarRat
            Else
                If TripRates = "Strategy" Then
                    PopRat = (((RlLinkExtVars(2, InputCount) + RlLinkExtVars(3, InputCount)) * RlTripRates(YearNum)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(1, YearNum)
                Else
                    PopRat = ((RlLinkExtVars(2, InputCount) + RlLinkExtVars(3, InputCount)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(1, YearNum)
                End If
            End If
            'gva ratio
            OldY = GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0)
            NewY = RlLinkExtVars(4, InputCount) + RlLinkExtVars(5, InputCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(2, YearNum)
                Call VarElCalc()
                GVARat = VarRat
            Else
                GVARat = ((RlLinkExtVars(4, InputCount) + RlLinkExtVars(5, InputCount)) / (GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0))) ^ RlLinkEl(2, YearNum)
            End If
            'delay ratio
            OldY = OldDelays(InputCount, 0)
            NewY = NewDelays(InputCount, 0)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(3, YearNum)
                Call VarElCalc()
                DelayRat = VarRat
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(3, YearNum)
            End If
            'cost ratio
            OldY = RlLinkCost(InputCount, 0)
            NewY = newcost
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(4, YearNum)
                Call VarElCalc()
                CostRat = VarRat
            Else
                CostRat = (newcost / RlLinkCost(InputCount, 0)) ^ RlLinkEl(4, YearNum)
            End If

            'car fuel ratio
            OldY = CarFuel(InputCount, 0)
            NewY = RlLinkExtVars(7, InputCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(5, YearNum)
                Call VarElCalc()
                CarFuelRat = VarRat
            Else
                CarFuelRat = (RlLinkExtVars(7, InputCount) / CarFuel(InputCount, 0)) ^ RlLinkEl(5, YearNum)
            End If
        Else
            If TripRates = "Strategy" Then
                PopRat = (((RlLinkExtVars(2, InputCount) + RlLinkExtVars(3, InputCount)) * RlTripRates(YearNum)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(1, YearNum)
            Else
                PopRat = ((RlLinkExtVars(2, InputCount) + RlLinkExtVars(3, InputCount)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(1, YearNum)
            End If

            GVARat = ((RlLinkExtVars(4, InputCount) + RlLinkExtVars(5, InputCount)) / (GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0))) ^ RlLinkEl(2, YearNum)
            DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(3, YearNum)
            CostRat = (newcost / RlLinkCost(InputCount, 0)) ^ RlLinkEl(4, YearNum)
            CarFuelRat = (RlLinkExtVars(7, InputCount) / CarFuel(InputCount, 0)) ^ RlLinkEl(5, YearNum)
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
                OldEl = RlLinkEl(3, YearNum)
                Call VarElCalc()
                DelayRat = VarRat
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(3, YearNum)
            End If
        Else
            DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(3, YearNum)
        End If
        '***TEST MOD
        OldDelays(InputCount, 0) = NewDelays(InputCount, 0)
    End Sub

    Sub WriteRlLinkOutput()
        Dim row As String

        FuelUsed(YearNum, 0) += ((1 - RlLinkExtVars(9, InputCount)) * NewTrains * 17695 * RlFuelEff(YearNum, 0))
        FuelUsed(YearNum, 1) += (RlLinkExtVars(9, InputCount) * NewTrains * 107421 * RlFuelEff(YearNum, 1))

        If CalcCheck(InputCount, 0) = True Then
            row = YearNum & "," & FlowNum(InputCount, 0) & "," & NewTrains & "," & NewDelays(InputCount, 0) & "," & CUNew(InputCount, 0)
            rao.WriteLine(row)
        Else
            row = YearNum & "," & FlowNum(InputCount, 0) & "," & RlLinkExtVars(11, InputCount) & ",1," & CUNew(InputCount, 0)
            rao.WriteLine(row)
            NewTrains = RlLinkExtVars(11, InputCount)
            NewDelays(InputCount, 0) = 1
        End If

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
                newcapstring = (YearNum + 1) & "," & FlowNum(InputCount, 0) & "," & newtrackcount
                ranc.WriteLine(newcapstring)
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

        PopZ1Base(InputCount, 0) = RlLinkExtVars(2, InputCount)
        PopZ2Base(InputCount, 0) = RlLinkExtVars(3, InputCount)
        GVAZ1Base(InputCount, 0) = RlLinkExtVars(4, InputCount)
        GVAZ2Base(InputCount, 0) = RlLinkExtVars(5, InputCount)
        OldDelays(InputCount, 0) = NewDelays(InputCount, 0)
        RlLinkCost(InputCount, 0) = newcost
        CarFuel(InputCount, 0) = RlLinkExtVars(7, InputCount)
        OldTrains(InputCount, 0) = NewTrains
        OldTracks(InputCount, 0) = NewTracks
        MaxTDBase(InputCount, 0) = MaxTDNew

        If CalcCheck(InputCount, 0) = True Then
            CalcCheck1 = 1
        Else
            CalcCheck1 = 0
        End If

        'write second row
        OutputRow = YearNum & "," & FlowNum(InputCount, 0) & "," & PopZ1Base(InputCount, 0) & "," & PopZ2Base(InputCount, 0) & "," & GVAZ1Base(InputCount, 0) & "," & GVAZ2Base(InputCount, 0) & "," & OldDelays(InputCount, 0) & "," & RlLinkCost(InputCount, 0) & "," & CarFuel(InputCount, 0) & "," & OldTrains(InputCount, 0) & "," & OldTracks(InputCount, 0) & "," & MaxTDBase(InputCount, 0) & "," & CUOld(InputCount, 0) & "," & CUNew(InputCount, 0) & "," & BusyTrains(InputCount, 0) & "," & BusyPer(InputCount, 0) & "," & ModelPeakHeadway(InputCount, 0) & "," & CalcCheck1 & "," & NewDelays(InputCount, 0) & ","

        rllw.WriteLine(OutputRow)

    End Sub
End Module