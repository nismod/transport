Module RailModel
    'now allows variable elasticities over time, specified via input file
    'now also has the option to build additional infrastructure if capacity exceeds a certain level
    'now also has the option to include a congestion-type charge
    'now also has the option to include a carbon-based charge
    'now includes option to base capacity utilisation on busiest hour
    'now also estimates fuel consumption
    'now also includes variable trip rate option

    'v1.6 now calculation by annual timesteps
    'need to think about FuelUsed(y,0) if the calculation are seperated for each year
    'two errors in the old code are identified: NEWCOST was not reseted for each link each year, external variable file doesn't read the complete RailLinkElectrificationDates (becasue to link260 year 2017 data)
    'v1.7 now corporate with Database function, read/write are using the function in database interface
    'now all file related functions are using databaseinterface
    '1.9 now the module can run with database connection and read/write from/to database

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
    Dim RlLinkExtVars(238, 21) As String
    Dim RlLinkPreExtVars(238, 21) As String
    Dim TrainRat As Double
    Dim PopRat As Double
    Dim GVARat As Double
    Dim DelayRat As Double
    Dim CostRat As Double
    Dim CarFuelRat As Double
    Dim SpdRat As Double
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
    Dim RlLinkEl(90, 7) As String
    Dim FlowNum(238, 0) As Integer
    Dim AddedTracks(238, 0) As Integer
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim Newcost, FixedCost As Double
    Dim CalcCheck(238, 0) As Boolean
    Dim BusyTrains(238, 0) As Long
    Dim BusyPer(238, 0) As Double
    Dim ModelPeakHeadway(238, 0) As Double
    Dim FuelUsed(1) As Double
    Dim RlFuelEff(1) As Double
    Dim FuelString(1, 4) As String
    Dim RlTripRates As Double
    Dim InputCount As Long
    Dim OutputRow As String
    Dim RlLinkLine As String
    Dim RlLinkArray() As String
    Dim CalcCheck1 As Integer
    Dim InputArray(238, 17) As String
    Dim InputArrayOld(238, 17) As String
    Dim OutputArray(239, 5) As String
    Dim TempArray(239, 15) As String
    Dim NewCapArray(239, 3) As String
    Dim NewCapNum As Integer
    Dim totalTrain As Double
    Dim totalCUTrain As Double
    Dim RlL_TrackLength(,) As String
    Dim OldSpd As Double
    Dim NewSpd As Double




    Public Sub RailLinkMain()


        'for year 2010
        If g_modelRunYear = 2010 Then
            'create data for year 2010
            'g_modelRunYear += 1
            Call Year2010()
            Exit Sub
        End If

        'read related files
        Call RailLinkInputFiles()

        'reset Cap number
        NewCapNum = 1
        'ReDim NewCapArray(239, 3)
        ReDim FuelUsed(1)

        'reset capacity margin count for the year
        'TotalCU=∑CU*Trains/∑Trains
        totalTrain = 0
        totalCUTrain = 0

        'read from initial file if year 1, otherwise update from temp table
        Call ReadData("RailLink", "Input", InputArray, g_modelRunYear)

        'get external variables for this year
        Call ReadData("RailLink", "ExtVar", RlLinkExtVars, g_modelRunYear)


        'get external variables from previous year as base data
        If g_modelRunYear <> g_initialYear Then
            Call ReadData("RailLink", "ExtVar", RlLinkPreExtVars, g_modelRunYear - 1)
            If BuildInfra = True Then
                'read the previous year's input to calculate the capacity added this year for TR1
                Call ReadData("RailLink", "Input", InputArrayOld, g_modelRunYear - 1)
                'read track length data in order to get the investment cost if there are tracks added in this year
                Call ReadData("RailLink", "TrackLength", RlL_TrackLength)
            End If
        End If


        InputCount = 1

        Do While InputCount < 239
            'update the input variables
            Call LoadRailLinkInput()

            'mod - check if number of tracks is greater than 0
            If RlLinkExtVars(InputCount, 4) > 0 Then
                'check if calculating check is false or true
                If CalcCheck(InputCount, 0) = True Then
                    'calculate constrained traffic level
                    Call ConstTrainCalc()

                    'write the flows to the output file
                    Call WriteRlLinkOutput()
                Else
                    'if not then this is the first year there have been track and trains on this route
                    If RailCUPeriod = "busy" Then
                        BusyPer(InputCount, 0) = 0.08
                    End If
                    'calculate CU
                    If RailCUPeriod = "busy" Then
                        CUNew(InputCount, 0) = ((RlLinkExtVars(InputCount, 14) * 0.08) / RlLinkExtVars(InputCount, 4)) / (60 / ModelPeakHeadway(InputCount, 0))
                    Else
                        CUNew(InputCount, 0) = (RlLinkExtVars(InputCount, 14) / RlLinkExtVars(InputCount, 4)) / RlLinkExtVars(InputCount, 11)
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

            InputCount += 1
        Loop

        'create file if it is the initial year, otherwise update to temp file table
        If g_modelRunYear = g_initialYear Then
            Call WriteData("RailLink", "Output", OutputArray, , True)
            Call WriteData("RailLink", "Temp", TempArray, , True)
            'if the model is building capacity then create new capacity file
            If BuildInfra = True And Not NewCapArray Is Nothing Then
                Call WriteData("RailLink", "NewCap_Added", NewCapArray, , True)
            End If
        Else
            Call WriteData("RailLink", "Output", OutputArray, , False)
            Call WriteData("RailLink", "Temp", TempArray, , False)
            If BuildInfra = True And Not NewCapArray Is Nothing Then
                Call WriteData("RailLink", "NewCap_Added", NewCapArray, , False)
            End If
        End If



        'Write fuel consumption output file
        FuelString(1, 0) = g_modelRunID
        FuelString(1, 1) = g_modelRunYear
        FuelString(1, 2) = FuelUsed(0) / 1000000
        FuelString(1, 3) = FuelUsed(1) / 1000000
        'total emission
        'Emissions = ((Diesely * CO2LDie) + (Electricy * CO2KEle))/1000
        FuelString(1, 4) = (CDbl(FuelString(1, 2)) * stratarray(1, 75) + CDbl(FuelString(1, 3)) * stratarray(1, 76)) / 1000

        'write to crossSector output
        crossSectorArray(1, 4) += CDbl(FuelString(1, 4))
        'adding the cu of rail to the aggregate capacity margin
        capacityMargin(1, 3) += ((totalCUTrain / totalTrain))

        Call WriteData("RailLink", "FuelUsed", FuelString)

    End Sub

    Sub RailLinkInputFiles()


        'get rail external variables file suffix
        If UpdateExtVars = True Then
            If NewRlLCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If

        'get the elasticity values
        Call ReadData("RailLink", "Elasticity", RlLinkEl, g_modelRunYear)


        'get fuel efficiency data from strategy file
        'also get trip rate info
        RlFuelEff(0) = stratarray(1, 69)
        RlFuelEff(1) = stratarray(1, 68)
        RlTripRates = stratarray(1, 95)

    End Sub

    Sub LoadRailLinkInput()
        Dim Zone1ID As Integer
        Dim Zone2ID As Integer

        If g_modelRunYear = g_initialYear Then
            'assign values to variables in the order of the initial file
            FlowNum(InputCount, 0) = InputArray(InputCount, 1)
            Zone1ID = InputArray(InputCount, 2)
            Zone2ID = InputArray(InputCount, 3)
            OldTracks(InputCount, 0) = InputArray(InputCount, 4)
            OldTrains(InputCount, 0) = InputArray(InputCount, 5)
            NewTrains = OldTrains(InputCount, 0)
            'read previous years' value as base value
            PopZ1Base(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, Zone1ID, "OZ", "'rail'")
            PopZ2Base(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, Zone2ID, "DZ", "'rail'")
            GVAZ1Base(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, Zone1ID, "OZ", "'rail'")
            GVAZ2Base(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, Zone2ID, "DZ", "'rail'")
            OldDelays(InputCount, 0) = InputArray(InputCount, 6)
            RlLinkCost(InputCount, 0) = InputArray(InputCount, 7)
            CarFuel(InputCount, 0) = InputArray(InputCount, 8)
            MaxTDBase(InputCount, 0) = InputArray(InputCount, 9)
            If RailCUPeriod = "busy" Then
                BusyTrains(InputCount, 0) = InputArray(InputCount, 14)
                If OldTrains(InputCount, 0) = 0 Then
                    BusyPer(InputCount, 0) = 0
                Else
                    BusyPer(InputCount, 0) = BusyTrains(InputCount, 0) / OldTrains(InputCount, 0)
                End If
                ModelPeakHeadway(InputCount, 0) = RlPeakHeadway
            End If

            AddedTracks(InputCount, 0) = 0
            CalcCheck(InputCount, 0) = True
            'set new delays value to equal base delay value to start with
            NewDelays(InputCount, 0) = OldDelays(InputCount, 0)
            OldSpd = InputArray(InputCount, 15)
        Else
            'assign values to variables in the order of the temp file
            FlowNum(InputCount, 0) = InputArray(InputCount, 3)

            'read OZone and DZone ID
            Call get_zone_by_flowid(FlowNum(InputCount, 0), Zone1ID, Zone2ID, "rail")

            'read previous years' value as base value
            PopZ1Base(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, Zone1ID, "OZ", "'rail'")
            PopZ2Base(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, Zone2ID, "DZ", "'rail'")
            GVAZ1Base(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, Zone1ID, "OZ", "'rail'")
            GVAZ2Base(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, Zone2ID, "DZ", "'rail'")
            OldDelays(InputCount, 0) = InputArray(InputCount, 4)
            RlLinkCost(InputCount, 0) = InputArray(InputCount, 5)
            'TODO This get_single_data should be replaced with a ReadData command
            'CarFuel(InputCount, 0) = get_single_data("TR_O_RailLinkExternalVariables", "flow_id", "year", Chr(34) & "CarFuel" & Chr(34), g_modelRunYear - 1, FlowNum(InputCount, 0))
            CarFuel(InputCount, 0) = RlLinkPreExtVars(InputCount, 10)

            OldTrains(InputCount, 0) = InputArray(InputCount, 6)
            NewTrains = OldTrains(InputCount, 0)
            OldTracks(InputCount, 0) = InputArray(InputCount, 7)
            MaxTDBase(InputCount, 0) = InputArray(InputCount, 8)


            CUOld(InputCount, 0) = InputArray(InputCount, 9)
            CUNew(InputCount, 0) = InputArray(InputCount, 10)

            BusyTrains(InputCount, 0) = InputArray(InputCount, 11)
            BusyPer(InputCount, 0) = InputArray(InputCount, 12)
            ModelPeakHeadway(InputCount, 0) = InputArray(InputCount, 13)
            CalcCheck1 = InputArray(InputCount, 14)

            'read CalculationCheck 
            If CalcCheck1 = 1 Then
                CalcCheck(InputCount, 0) = True
            Else
                CalcCheck(InputCount, 0) = False
            End If

            If InputArray(InputCount, 15) = "" Then
                InputArray(InputCount, 15) = 0
            End If
            AddedTracks(InputCount, 0) = InputArray(InputCount, 15)

            OldSpd = InputArray(InputCount, 16)

            'add the cost of the infrastructure built
            If BuildInfra = True Then
                If g_modelRunYear = 2012 Then
                    crossSectorArray(1, 3) += 18.64 * AddedTracks(InputCount, 0) * RlL_TrackLength(InputCount + 1, 4) 'the inputcount must be +1, as the first row is for id = -1 in the table
                ElseIf g_modelRunYear > 2012 Then
                    crossSectorArray(1, 3) += 18.64 * (CDbl(AddedTracks(InputCount, 0)) - InputArrayOld(InputCount, 15)) * RlL_TrackLength(InputCount + 1, 4) 'the inputcount must be +1, as the first row is for id = -1 in the table
                End If
            End If

        End If

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
        NewTracks = RlLinkExtVars(InputCount, 4) + AddedTracks(InputCount, 0)
        MaxTDNew = RlLinkExtVars(InputCount, 11)
        If RailCUPeriod = "busy" Then
            If MaxTDNew <> MaxTDBase(InputCount, 0) Then
                ModelPeakHeadway(InputCount, 0) = ModelPeakHeadway(InputCount, 0) / (MaxTDNew / MaxTDBase(InputCount, 0))
            End If
        End If
        Call DelayCalc()

        'if CUNew is greater than 1 (ie over the maximum), then set the number of trains to be equal to the maximum and move on
        '***arguably should store the latent demand - but not doing this currently
        If CUNew(InputCount, 0) > 1 Then
            If RailCUPeriod = "busy" Then
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
                    OldEl = RlLinkEl(1, 5)
                    Call VarElCalc()
                    DelayRat = VarRat
                Else
                    DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(1, 5)
                End If
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(1, 5)
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

                    'write to effort log
                    Dim msg As String = "Flow " & FlowNum(InputCount, 0) & " failed to converge in Year " & g_modelRunYear & " in rail link model.  Best convergence ratio was " & CloseTrainRat & "."
                    Call ErrorLog(ErrorSeverity.FATAL, "ConsTrainCalc", "RailModel", msg)
                    Throw New System.Exception(msg)
                    Exit Do
                End If
                'If TrainRat becomes Not A Number then error out
                If Double.IsNaN(TrainRat) Then
                    'write to effort log
                    Dim msg As String = "Flow " & FlowNum(InputCount, 0) & " became Not A Number in Year " & g_modelRunYear & " of the rail link model."
                    Call ErrorLog(ErrorSeverity.FATAL, "ConsTrainCalc", "RailModel", msg)
                    Throw New System.Exception(msg)
                    Exit Do
                End If
            Loop
        End If

        'if there are additional trains from the external variables file then add them here
        If RlLinkExtVars(InputCount, 14) > 0 Then
            NewTrains += RlLinkExtVars(InputCount, 14)
        End If

    End Sub

    Sub TrainNumCalc()
        Dim basecost As Double

        'now includes option to have congestion charge
        basecost = RlLinkExtVars(InputCount, 9)
        FixedCost = 121.381 + (RlLinkExtVars(InputCount, 12) * 24.855) + ((1 - RlLinkExtVars(InputCount, 12)) * 37.282)
        If RailCCharge = True Then
            If g_modelRunYear >= RlCChargeYear Then
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
            If TripRates = True Then
                NewY = (CDbl(RlLinkExtVars(InputCount, 5)) + RlLinkExtVars(InputCount, 6)) * RlTripRates
            Else
                NewY = CDbl(RlLinkExtVars(InputCount, 6)) + RlLinkExtVars(InputCount, 6)
            End If

            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(1, 3)
                Call VarElCalc()
                PopRat = VarRat
            Else
                If TripRates = True Then
                    PopRat = (((CDbl(RlLinkExtVars(InputCount, 5)) + RlLinkExtVars(InputCount, 6)) * RlTripRates) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(1, 3)
                Else
                    PopRat = ((CDbl(RlLinkExtVars(InputCount, 5)) + RlLinkExtVars(InputCount, 6)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(1, 3)
                End If
            End If
            'gva ratio
            OldY = GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0)
            NewY = CDbl(RlLinkExtVars(InputCount, 7)) + RlLinkExtVars(InputCount, 8)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(1, 4)
                Call VarElCalc()
                GVARat = VarRat
            Else
                GVARat = ((CDbl(RlLinkExtVars(InputCount, 7)) + RlLinkExtVars(InputCount, 8)) / (GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0))) ^ RlLinkEl(1, 4)
            End If
            'delay ratio
            OldY = OldDelays(InputCount, 0)
            NewY = NewDelays(InputCount, 0)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(1, 5)
                Call VarElCalc()
                DelayRat = VarRat
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(1, 5)
            End If
            'cost ratio
            OldY = RlLinkCost(InputCount, 0)
            NewY = newcost
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(1, 6)
                Call VarElCalc()
                CostRat = VarRat
            Else
                CostRat = (Newcost / RlLinkCost(InputCount, 0)) ^ RlLinkEl(1, 6)
            End If

            'car fuel ratio
            OldY = CarFuel(InputCount, 0)
            NewY = RlLinkExtVars(InputCount, 10)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlLinkEl(1, 7)
                Call VarElCalc()
                CarFuelRat = VarRat
            Else
                CarFuelRat = (RlLinkExtVars(InputCount, 10) / CarFuel(InputCount, 0)) ^ RlLinkEl(1, 7)
            End If
        Else
            If TripRates = True Then
                PopRat = (((CDbl(RlLinkExtVars(InputCount, 5)) + RlLinkExtVars(InputCount, 6)) * RlTripRates) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(1, 3)
            Else
                PopRat = ((CDbl(RlLinkExtVars(InputCount, 5)) + RlLinkExtVars(InputCount, 6)) / (PopZ1Base(InputCount, 0) + PopZ2Base(InputCount, 0))) ^ RlLinkEl(1, 3)
            End If

            GVARat = ((CDbl(RlLinkExtVars(InputCount, 7)) + RlLinkExtVars(InputCount, 8)) / (GVAZ1Base(InputCount, 0) + GVAZ2Base(InputCount, 0))) ^ RlLinkEl(1, 4)
            DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(1, 5)
            CostRat = (Newcost / RlLinkCost(InputCount, 0)) ^ RlLinkEl(1, 6)
            CarFuelRat = (CDbl(RlLinkExtVars(InputCount, 10)) / CarFuel(InputCount, 0)) ^ RlLinkEl(1, 7)
        End If

        'speed ratio
        NewSpd = (CDbl(RlLinkExtVars(InputCount, 19)) / OldTrains(InputCount, 0)) * RlLinkExtVars(InputCount, 21) + ((OldTrains(InputCount, 0) - CDbl(RlLinkExtVars(InputCount, 19))) / OldTrains(InputCount, 0)) * OldSpd

        SpdRat = (NewSpd / OldSpd) ^ 1.2


        NewTrains = OldTrains(InputCount, 0) * PopRat * GVARat * DelayRat * CostRat * CarFuelRat * SpdRat

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
        If RailCUPeriod = "busy" Then
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
                OldEl = RlLinkEl(1, 5)
                Call VarElCalc()
                DelayRat = VarRat
            Else
                DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(1, 5)
            End If
        Else
            DelayRat = (NewDelays(InputCount, 0) / OldDelays(InputCount, 0)) ^ RlLinkEl(1, 5)
        End If
        '***TEST MOD
        OldDelays(InputCount, 0) = NewDelays(InputCount, 0)
    End Sub

    Sub WriteRlLinkOutput()
        'update fuelused
        FuelUsed(0) += ((1 - RlLinkExtVars(InputCount, 12)) * NewTrains * 17695 * RlFuelEff(0))
        FuelUsed(1) += (RlLinkExtVars(InputCount, 12) * NewTrains * 107421 * RlFuelEff(1))

        'write to outputarray

        If CalcCheck(InputCount, 0) = True Then
            OutputArray(InputCount, 0) = g_modelRunID
            OutputArray(InputCount, 1) = FlowNum(InputCount, 0)
            OutputArray(InputCount, 2) = g_modelRunYear
            ''TODO update newtrain every year, otherwise it will read the previous link value
            ''now it forces the output to be the old output value, need to fix this
            'If g_modelRunYear = 2016 Then
            '    If InputCount = 177 Then
            '        NewTrains = 36
            '    End If
            'End If
            If RlLinkPreExtVars(InputCount, 4) = 0 And Not RlLinkPreExtVars(InputCount, 4) Is Nothing Then NewTrains = RlLinkExtVars(InputCount, 14)
            OutputArray(InputCount, 3) = NewTrains
            OutputArray(InputCount, 4) = NewDelays(InputCount, 0)
            OutputArray(InputCount, 5) = CUNew(InputCount, 0)
        Else
            OutputArray(InputCount, 0) = g_modelRunID
            OutputArray(InputCount, 1) = FlowNum(InputCount, 0)
            OutputArray(InputCount, 2) = g_modelRunYear
            OutputArray(InputCount, 3) = RlLinkExtVars(InputCount, 14)
            OutputArray(InputCount, 4) = 1
            OutputArray(InputCount, 5) = CUNew(InputCount, 0)
            NewTrains = RlLinkExtVars(InputCount, 14)
            NewDelays(InputCount, 0) = 1
        End If

        'update capacity margin
        'TotalCU=∑CU*Trains/∑Trains
        totalCUTrain += CDbl(OutputArray(InputCount, 5)) * OutputArray(InputCount, 3)
        totalTrain += OutputArray(InputCount, 3)

        'update variables
        OldDelays(InputCount, 0) = NewDelays(InputCount, 0)
        RlLinkCost(InputCount, 0) = Newcost
        OldTrains(InputCount, 0) = NewTrains
        OldTracks(InputCount, 0) = NewTracks
        MaxTDBase(InputCount, 0) = MaxTDNew

        If CalcCheck(InputCount, 0) = True Then
            CalcCheck1 = 1
        Else
            CalcCheck1 = 0
        End If

        'reset the input values for the next year
        Call ResetRailInputs()

        'write to temparray
        TempArray(InputCount, 0) = g_modelRunID
        TempArray(InputCount, 1) = g_modelRunYear
        TempArray(InputCount, 2) = FlowNum(InputCount, 0)

        TempArray(InputCount, 3) = OldDelays(InputCount, 0)
        TempArray(InputCount, 4) = RlLinkCost(InputCount, 0)
        TempArray(InputCount, 5) = OldTrains(InputCount, 0)
        TempArray(InputCount, 6) = OldTracks(InputCount, 0)
        TempArray(InputCount, 7) = MaxTDBase(InputCount, 0)
        TempArray(InputCount, 8) = InputArray(InputCount, 12)
        TempArray(InputCount, 9) = InputArray(InputCount, 13)

        TempArray(InputCount, 10) = BusyTrains(InputCount, 0)
        TempArray(InputCount, 11) = BusyPer(InputCount, 0)
        TempArray(InputCount, 12) = ModelPeakHeadway(InputCount, 0)
        TempArray(InputCount, 13) = CalcCheck1
        TempArray(InputCount, 14) = AddedTracks(InputCount, 0)
        TempArray(InputCount, 15) = NewSpd
    End Sub

    Sub ResetRailInputs()
        'v1.6 now the reset function moved to Write/Read temp file
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
                NewCapArray(NewCapNum, 0) = g_modelRunID
                NewCapArray(NewCapNum, 1) = FlowNum(InputCount, 0)
                NewCapArray(NewCapNum, 2) = g_modelRunYear + 1
                NewCapArray(NewCapNum, 3) = newtrackcount
                NewCapNum += 1
            End If
        End If


    End Sub

    Sub Year2010()
        Call ReadData("RailLink", "Input", InputArray, g_modelRunYear + 1)
        'Call ReadData("RailLink", "ExtVar", RlLinkExtVars, (g_modelRunYear + 1))

        InputCount = 1

        Do While InputCount < 239
            OutputArray(InputCount, 0) = g_modelRunID
            OutputArray(InputCount, 1) = InputArray(InputCount, 1)
            OutputArray(InputCount, 2) = g_modelRunYear
            OutputArray(InputCount, 3) = InputArray(InputCount, 5)
            OutputArray(InputCount, 4) = InputArray(InputCount, 6)
            If RailCUPeriod = "busy" Then
                BusyTrains(InputCount, 0) = InputArray(InputCount, 14)
                If OldTrains(InputCount, 0) = 0 Then
                    BusyPer(InputCount, 0) = 0
                Else
                    BusyPer(InputCount, 0) = BusyTrains(InputCount, 0) / OldTrains(InputCount, 0)
                End If
                ModelPeakHeadway(InputCount, 0) = RlPeakHeadway
            End If
            If RailCUPeriod = "busy" Then
                'OutputArray(InputCount, 5) = ((RlLinkExtVars(InputCount, 14) * 0.08) / RlLinkExtVars(InputCount, 4)) / (60 / ModelPeakHeadway(InputCount, 0))
                OutputArray(InputCount, 5) = 1
            Else
                'OutputArray(InputCount, 5) = (RlLinkExtVars(InputCount, 14) / RlLinkExtVars(InputCount, 4)) / RlLinkExtVars(InputCount, 11)
                OutputArray(InputCount, 5) = 1
            End If

            InputCount += 1
        Loop

        Call WriteData("RailLink", "Output", OutputArray, , True)

        'Write fuel consumption output file
        FuelString(1, 0) = g_modelRunID
        FuelString(1, 1) = g_modelRunYear
        FuelString(1, 2) = 667488883.969476 / 1000000
        FuelString(1, 3) = 3183754707.28527 / 1000000
        FuelString(1, 4) = (667488883.969476 / 1000000 * stratarray(1, 75) + 3183754707.28527 / 1000000 * stratarray(1, 76)) / (1000)
        'write to crossSector output
        crossSectorArray(1, 4) += CDbl(FuelString(1, 4))

        Call WriteData("RailLink", "FuelUsed", FuelString)

    End Sub

End Module