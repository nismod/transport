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
    Dim PopZ1Base As Double
    Dim PopZ2Base As Double
    Dim GVAZ1Base As Double
    Dim GVAZ2Base As Double
    Dim Delays As Double
    Dim RlLinkCost As Double
    Dim CarFuel As Double
    Dim RlLinkExtVars(11, 90) As Double
    Dim YearNum As Long
    Dim TrainRat As Double
    Dim PopRat As Double
    Dim GVARat As Double
    Dim DelayRat As Double
    Dim CostRat As Double
    Dim CarFuelRat As Double
    Dim OldDelays As Double
    Dim NewDelays As Double
    Dim OldTrains As Double
    Dim NewTrains As Double
    Dim OldTracks As Double
    Dim NewTracks As Double
    Dim CUOld As Double
    Dim CUNew As Double
    Dim MaxTDBase As Double
    Dim MaxTDNew As Double
    Dim RlLinkEl(5, 90) As Double
    Dim FlowNum As Integer
    Dim AddedTracks As Integer
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim NewCost, FixedCost As Double
    Dim CalcCheck As Boolean
    Dim BusyTrains As Long
    Dim BusyPer As Double
    Dim ModelPeakHeadway As Double
    Dim FuelUsed(90, 1) As Double
    Dim RlFuelEff(90, 1) As Double
    Dim FuelString As String
    Dim RlTripRates(90) As Double

    Public Sub RailLinkMain()
        'get the input files and create the output files
        Call RailLinkInputFiles()

        'get the elasticity values
        Call RailLinkElasticities()

        'loop through all the links in the input file
        Do
            InputRow = rai.ReadLine
            'check if at end of file
            If InputRow Is Nothing Then
                'if we are then exit loop
                Exit Do
                'if not then run model on this link
            Else
                AddedTracks = 0
                CalcCheck = True
                'update the input variables
                Call LoadRailLinkInput()
                'get external variables for this link
                Call GetRailLinkExtVar()
                'set year number to 1 (equivalent to 2011)
                YearNum = 1
                'set new delays value to equal base delay value to start with
                NewDelays = OldDelays
                'loop through all years calculating new rail traffic and writing to output file
                Do While YearNum < 91
                    'mod - check if number of tracks is greater than 0
                    If RlLinkExtVars(1, YearNum) > 0 Then
                        'check if calculating check is false or true
                        If CalcCheck = True Then
                            'calculate constrained traffic level
                            Call ConstTrainCalc()
                            'write the flows to the output file
                            Call WriteRlLinkOutput()
                        Else
                            'if not then this is the first year there have been track and trains on this route
                            If RailCUPeriod = "Hour" Then
                                BusyPer = 0.08
                            End If
                            'calculate CU
                            If RailCUPeriod = "Hour" Then
                                CUNew = ((RlLinkExtVars(11, YearNum) * 0.08) / RlLinkExtVars(1, YearNum)) / (60 / ModelPeakHeadway)
                            Else
                                CUNew = (RlLinkExtVars(11, YearNum) / RlLinkExtVars(1, YearNum)) / RlLinkExtVars(8, YearNum)
                            End If
                            'write the flows to the output file
                            Call WriteRlLinkOutput()
                            CalcCheck = True
                        End If
                    Else
                        'if not set calculating check to false
                        CalcCheck = False
                        CUNew = 0
                        'write the flows to the output file
                        Call WriteRlLinkOutput()
                    End If
                    'reset the input values for the next year
                    Call ResetRailInputs()
                    'move on to next year
                    YearNum += 1
                Loop
            End If
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
        rao.WriteLine("FlowID,Yeary,Trainsy,Delaysy,CUy")

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
            ranc.WriteLine("FlowID,Yeary,TracksAdded")
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
        RlLinkDetails = Split(InputRow, ",")
        'assign values to variables
        FlowNum = RlLinkDetails(0)
        OldTracks = RlLinkDetails(3)
        OldTrains = RlLinkDetails(4)
        PopZ1Base = RlLinkDetails(5)
        PopZ2Base = RlLinkDetails(6)
        GVAZ1Base = RlLinkDetails(7)
        GVAZ2Base = RlLinkDetails(8)
        OldDelays = RlLinkDetails(9)
        RlLinkCost = RlLinkDetails(10)
        CarFuel = RlLinkDetails(11)
        MaxTDBase = RlLinkDetails(12)
        If RailCUPeriod = "Hour" Then
            BusyTrains = RlLinkDetails(17)
            BusyPer = BusyTrains / OldTrains
            ModelPeakHeadway = RlPeakHeadway
        End If

    End Sub

    Sub GetRailLinkExtVar()
        Dim yearcheck As Long
        Dim row As String
        Dim linedetails() As String
        Dim item As Integer

        yearcheck = 1
        Do While yearcheck < 91
            row = rae.ReadLine
            linedetails = Split(row, ",")
            item = 2
            Do Until item = 13
                RlLinkExtVars(item - 1, yearcheck) = linedetails(item)
                item += 1
            Loop
            yearcheck += 1
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
        NewTracks = RlLinkExtVars(1, YearNum) + AddedTracks
        MaxTDNew = RlLinkExtVars(8, YearNum)
        If RailCUPeriod = "Hour" Then
            If MaxTDNew <> MaxTDBase Then
                ModelPeakHeadway = ModelPeakHeadway / (MaxTDNew / MaxTDBase)
            End If
        End If
        Call DelayCalc()
        'if CUNew is greater than 1 (ie over the maximum), then set the number of trains to be equal to the maximum and move on
        '***arguably should store the latent demand - but not doing this currently
        If CUNew > 1 Then
            If RailCUPeriod = "Hour" Then
                NewTrains = NewTracks * (60 / ModelPeakHeadway) / BusyPer
            Else
                NewTrains = NewTracks * MaxTDNew
            End If
            CUNew = 1
            NewDelays = OldDelays * ((Math.Exp(2 * CUNew)) / (Math.Exp(2 * CUOld)))
            If VariableEl = True Then
                OldX = OldTrains
                OldY = OldDelays
                NewY = NewDelays
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = RlLinkEl(3, YearNum)
                    Call VarElCalc()
                    DelayRat = VarRat
                Else
                    DelayRat = (NewDelays / OldDelays) ^ RlLinkEl(3, YearNum)
                End If
            Else
                DelayRat = (NewDelays / OldDelays) ^ RlLinkEl(3, YearNum)
            End If
        Else
            'recalculate growth subject to delay constraint
            OldTrains = NewTrains
            NewTrains = OldTrains * DelayRat
            TrainRat = NewTrains / OldTrains
            'set old tracks equal to new tracks as don't want to change these further
            OldTracks = NewTracks
            'check whether convergence reached - if not then iterate between equations until it is
            Do Until TrainRat >= 0.99 And TrainRat <= 1.01
                'reestimate delays
                Call DelayCalc()
                'recalculate growth subject to delay constraint
                OldTrains = NewTrains
                NewTrains = OldTrains * DelayRat
                'this section is for catching flows that don't converge - although this should no longer be necessary now that we've imposed a maximum capacity constraint
                TrainRat = NewTrains / OldTrains
                RatCheck = Math.Abs(1 - TrainRat)
                If RatCheck < CloseTrainRat Then
                    CloseTrainRat = RatCheck
                    BestTrains = NewTrains
                    BestDelays = NewDelays
                ElseIf (RatCheck - CloseTrainRat) > 0.1 Then
                    NewTrains = BestTrains
                    NewDelays = BestDelays
                    LogLine = "Flow " & FlowNum & " failed to converge in Year " & YearNum & " in rail link model.  Best convergence ratio was " & CloseTrainRat & "."
                    lf.WriteLine(LogLine)
                    Exit Do
                Else
                End If
                'this is the end of the section which could arguably be deleted
            Loop
        End If

        'if there are additional trains from the external variables file then add them here
        If RlLinkExtVars(11, YearNum) > 0 Then
            NewTrains += RlLinkExtVars(11, YearNum)
        End If

    End Sub

    Sub TrainNumCalc()
        Dim basecost As Double

        'now includes option to have congestion charge
        basecost = RlLinkExtVars(6, YearNum)
        FixedCost = 121.381 + (RlLinkExtVars(9, YearNum) * 24.855) + ((1 - RlLinkExtVars(9, YearNum)) * 37.282)
        If RailCCharge = True Then
            If YearNum >= RlCChargeYear Then
                If CUOld >= 0.25 Then
                    NewCost = basecost + (0.6461 * FixedCost * RailChargePer * (CUOld ^ 2))
                Else
                    NewCost = basecost
                End If
            Else
                NewCost = basecost
            End If
        Else
            NewCost = basecost
        End If
        'now includes option to use variable elasticities
        If VariableEl = True Then
            OldX = OldTrains
            'pop ratio
            OldY = PopZ1Base + PopZ2Base
            If TripRates = "Strategy" Then
                NewY = (RlLinkExtVars(2, YearNum) + RlLinkExtVars(3, YearNum)) * RlTripRates(YearNum)
            Else
                NewY = RlLinkExtVars(2, YearNum) + RlLinkExtVars(3, YearNum)
            End If

            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(1, YearNum)
                Call VarElCalc()
                PopRat = VarRat
            Else
                If TripRates = "Strategy" Then
                    PopRat = (((RlLinkExtVars(2, YearNum) + RlLinkExtVars(3, YearNum)) * RlTripRates(YearNum)) / (PopZ1Base + PopZ2Base)) ^ RlLinkEl(1, YearNum)
                Else
                    PopRat = ((RlLinkExtVars(2, YearNum) + RlLinkExtVars(3, YearNum)) / (PopZ1Base + PopZ2Base)) ^ RlLinkEl(1, YearNum)
                End If
            End If
            'gva ratio
            OldY = GVAZ1Base + GVAZ2Base
            NewY = RlLinkExtVars(4, YearNum) + RlLinkExtVars(5, YearNum)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(2, YearNum)
                Call VarElCalc()
                GVARat = VarRat
            Else
                GVARat = ((RlLinkExtVars(4, YearNum) + RlLinkExtVars(5, YearNum)) / (GVAZ1Base + GVAZ2Base)) ^ RlLinkEl(2, YearNum)
            End If
            'delay ratio
            OldY = OldDelays
            NewY = NewDelays
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(3, YearNum)
                Call VarElCalc()
                DelayRat = VarRat
            Else
                DelayRat = (NewDelays / OldDelays) ^ RlLinkEl(3, YearNum)
            End If
            'cost ratio
            OldY = RlLinkCost
            NewY = NewCost
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(4, YearNum)
                Call VarElCalc()
                CostRat = VarRat
            Else
                CostRat = (NewCost / RlLinkCost) ^ RlLinkEl(4, YearNum)
            End If
            'car fuel ratio
            OldY = CarFuel
            NewY = RlLinkExtVars(7, YearNum)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(5, YearNum)
                Call VarElCalc()
                CarFuelRat = VarRat
            Else
                CarFuelRat = (RlLinkExtVars(7, YearNum) / CarFuel) ^ RlLinkEl(5, YearNum)
            End If
        Else
            If TripRates = "Strategy" Then
                PopRat = (((RlLinkExtVars(2, YearNum) + RlLinkExtVars(3, YearNum)) * RlTripRates(YearNum)) / (PopZ1Base + PopZ2Base)) ^ RlLinkEl(1, YearNum)
            Else
                PopRat = ((RlLinkExtVars(2, YearNum) + RlLinkExtVars(3, YearNum)) / (PopZ1Base + PopZ2Base)) ^ RlLinkEl(1, YearNum)
            End If

            GVARat = ((RlLinkExtVars(4, YearNum) + RlLinkExtVars(5, YearNum)) / (GVAZ1Base + GVAZ2Base)) ^ RlLinkEl(2, YearNum)
            DelayRat = (NewDelays / OldDelays) ^ RlLinkEl(3, YearNum)
            CostRat = (NewCost / RlLinkCost) ^ RlLinkEl(4, YearNum)
            CarFuelRat = (RlLinkExtVars(7, YearNum) / CarFuel) ^ RlLinkEl(5, YearNum)
        End If
        NewTrains = OldTrains * PopRat * GVARat * DelayRat * CostRat * CarFuelRat
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
            CUOld = ((OldTrains * BusyPer) / OldTracks) / (60 / ModelPeakHeadway)
            CUNew = ((NewTrains * BusyPer) / NewTracks) / (60 / ModelPeakHeadway)
        Else
            CUOld = (OldTrains / OldTracks) / MaxTDBase
            CUNew = (NewTrains / NewTracks) / MaxTDNew
        End If
        NewDelays = OldDelays * ((Math.Exp(2 * CUNew)) / (Math.Exp(2 * CUOld)))
        'now includes option to use variable elasticities
        If VariableEl = True Then
            OldX = OldTrains
            OldY = OldDelays
            NewY = NewDelays
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(3, YearNum)
                Call VarElCalc()
                DelayRat = VarRat
            Else
                DelayRat = (NewDelays / OldDelays) ^ RlLinkEl(3, YearNum)
            End If
        Else
            DelayRat = (NewDelays / OldDelays) ^ RlLinkEl(3, YearNum)
        End If
        '***TEST MOD
        OldDelays = NewDelays
    End Sub

    Sub WriteRlLinkOutput()
        Dim row As String

        FuelUsed(YearNum, 0) += ((1 - RlLinkExtVars(9, YearNum)) * NewTrains * 17695 * RlFuelEff(YearNum, 0))
        FuelUsed(YearNum, 1) += (RlLinkExtVars(9, YearNum) * NewTrains * 107421 * RlFuelEff(YearNum, 1))

        If CalcCheck = True Then
            row = RlLinkDetails(0) & "," & YearNum & "," & NewTrains & "," & NewDelays & "," & CUNew
            rao.WriteLine(row)
        Else
            row = RlLinkDetails(0) & "," & YearNum & "," & RlLinkExtVars(11, YearNum) & ",1," & CUNew
            rao.WriteLine(row)
            NewTrains = RlLinkExtVars(11, YearNum)
            NewDelays = 1
        End If

    End Sub

    Sub ResetRailInputs()

        Dim newcapstring As String
        Dim newtrackcount As Integer

        PopZ1Base = RlLinkExtVars(2, YearNum)
        PopZ2Base = RlLinkExtVars(3, YearNum)
        GVAZ1Base = RlLinkExtVars(4, YearNum)
        GVAZ2Base = RlLinkExtVars(5, YearNum)
        OldDelays = NewDelays
        RlLinkCost = NewCost
        CarFuel = RlLinkExtVars(7, YearNum)
        OldTrains = NewTrains
        OldTracks = NewTracks
        MaxTDBase = MaxTDNew

        'if building capacity then check if new capacity is needed
        If BuildInfra = True Then
            If CUNew >= CUCritValue Then
                'if single track then add 1 track
                If OldTracks < 2 Then
                    OldTracks += 1
                    AddedTracks += 1
                    newtrackcount = 1
                Else
                    'otherwise add 2 tracks
                    OldTracks += 2
                    AddedTracks += 2
                    newtrackcount = 2
                End If
                'write details to output file
                newcapstring = FlowNum & "," & (YearNum + 1) & "," & newtrackcount
                ranc.WriteLine(newcapstring)
            End If
        End If
        
    End Sub
End Module