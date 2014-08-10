Module AirModel1pt4
    'v1.2 fuel consumption estimation added
    'this version works, and is dependent on the Full CDAM module for file path definitions
    'it now also allows for variable elasticities over time, specified via input file
    'now also has the option to build additional infrastructure if capacity exceeds a certain level
    'now has the option to use model-defined variable elasticities
    'now includes a congestion charging option
    'v1.3 reworked to try to eliminate inconsistencies
    'v1.4 further reworked, so that if a node or flow is constrained then traffic on that flow is 'locked' at a constant level until a capacity enhancement arrives
    'now also includes variable trip rate option
    '**may need to add in a further refinement so that if demand for that flow/node actually decreases in a given year then it can drop even if it is 'locked'
    'v1.7 now work with the databse interface functions for read/write data
    'now all file related functions are using databaseinterface

    Dim AirNodeInputData As IO.FileStream
    Dim an As IO.StreamReader
    Dim AirNodeExtVar As IO.FileStream
    Dim ne As IO.StreamReader
    Dim AirFlowInputData As IO.FileStream
    Dim af As IO.StreamReader
    Dim AirLinkExtVar As IO.FileStream
    Dim fe As IO.StreamReader
    Dim AirNodeOutputData As IO.FileStream
    Dim ao As IO.StreamWriter
    Dim AirFlowOutputData As IO.FileStream
    Dim fo As IO.StreamWriter
    Dim AirElasticities As IO.FileStream
    Dim ael As IO.StreamReader
    Dim AirNewCap As IO.FileStream
    Dim anc As IO.StreamWriter
    Dim asf As IO.StreamReader
    Dim YearNum As Long
    Dim NodeInput As String
    Dim NodeDetails() As String
    Dim FlowInput As String
    Dim FlowDetails() As String
    Dim AirportCount As Integer
    Dim AirportBaseData(28, 13) As Double
    Dim AirportExtVar(28, 12) As String
    Dim AirpPopRat, AirpGvaRat, AirpCostRat As Double
    Dim AirpTripRat As Double
    Dim AirpTripsNew(28) As Double
    Dim AirFlowBaseData(223, 9) As Double
    Dim AirFlowExtVar(223, 6) As String
    Dim FlowCount As Integer
    Dim AirfPopRat, AirfGvaRat, AirfCostRat As Double
    Dim AirfTripRat As Double
    Dim AirfTripsNew(223) As Double
    Dim AirpDomTripsNew(28) As Double
    Dim AirpPass(28) As Double
    Dim AirpATM(28) As Double
    Dim AirpTermCap(28) As Double
    Dim AirpTermCapCheck(28) As Boolean
    Dim AirpRunCapCheck(28) As Boolean
    Dim GrowthLimit As Boolean
    Dim c As Integer
    Dim airpid As Integer
    Dim AirpTripsLatent(28) As Double
    Dim AirfTripsLatent(223) As Double
    Dim AirpDomTripsLatent(28) As Double
    'this specifies whether a flow is constrained at the origin end or the destination end
    Dim AirfCapConst(223, 1) As Boolean
    Dim AirEl(90, 6) As String
    Dim AirpIntFuel(28) As Double
    Dim AirfFuel(223) As Double
    Dim AirpAddedCap(28, 2) As Long
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim AirpCapU(28, 3) As Double
    Dim AirpNonFuelCost(28) As Double
    Dim FlowCharge(223) As Double
    Dim AirpOldConstraint(28), AirfOldConstraint(223) As Boolean
    Dim AirTripRates(90) As Double
    Dim MaxAirNode As Long
    Dim MaxAirFlow As Long
    Dim NodeInputArray(28, 13) As String
    Dim FlowInputArray(223, 9) As String
    Dim NodeOutputArray(28, 6) As String
    Dim FlowOutputArray(223, 3) As String
    Dim NodeTempArray(28, 14) As String
    Dim FlowTempArray(223, 12) As String
    Dim NewCapArray(56, 3) As String



    Public Sub AirMain()

        'define the maximum node and flowID
        MaxAirNode = 28
        MaxAirFlow = 223


        'read all related files
        Call AirInputFiles()


        'Set year number to initial year
        YearNum = StartYear

        'Set capacity constraint checkers to false
        c = 0
        Do While c < 29
            AirpTermCapCheck(c) = False
            AirpRunCapCheck(c) = False
            c += 1
        Loop

        'loop through all 90 years of modelling period - this is the outside loop for this model because need to aggregate across flows each year
        Do Until YearNum > StartYear + Duration

            'read from initial file if year 1, otherwise update from temp file
            If YearNum = 1 Then
                Call ReadData("AirNode", "Input", NodeInputArray, True)
                Call ReadData("AirFlow", "Input", FlowInputArray, True)
            Else
                ReDim Preserve NodeInputArray(28, 14)
                ReDim Preserve FlowInputArray(223, 12)
                Call ReadData("AirNode", "Input", NodeInputArray, False)
                Call ReadData("AirFlow", "Input", FlowInputArray, False)
            End If

            'run air node model

            'get external variables for relevant year for all airports
            Call GetAirportExtVar()

            'get airport input from inputarray
            Call GetAirportInputData()

            '1.3 addition to allow capacity-based charging
            'calculate airport capacity utilisation values
            Call CalcAirpCapU()

            'calculate new airport international passenger totals
            Call AirNodeChange()




            'run air flow model

            'get external variables for relevant year for all flows
            Call GetAirFlowExtVar()

            'get air flow data from input file
            Call GetAirFlowInputData()

            'calculate new air flow totals, and sum them to give airport domestic traffic totals
            'add new flow level from each flow to airport total domestic flow value contained in an array
            'note that the airport domestic flow calculations are based on 
            Call AirFlowChange()

            'for each airport check if the growth means that capacity limits have been reached, and if so apply the constraints
            'this then reestimates constrained flows and nodal totals as required
            Call AirConstraints()

            'estimate fuel consumpion
            Call AirFuelConsumption()



            'write output values and temp array
            Call AirOutputValues()

            '***note - do we want to alter this to cope with reductions in capacity? - also with reductions in load factors and/or aircraft sizes?


            'create file is true if it is the initial year and write to outputfile and temp file
            If YearNum = StartYear Then
                Call WriteData("AirNode", "Output", NodeOutputArray, NodeTempArray, True)
                Call WriteData("AirFlow", "Output", FlowOutputArray, FlowTempArray, True)
                If BuildInfra = True Then
                    Call WriteData("AirNode", "AirNewCap", NewCapArray, , True)
                End If
            Else
                Call WriteData("AirNode", "Output", NodeOutputArray, NodeTempArray, False)
                Call WriteData("AirFlow", "Output", FlowOutputArray, FlowTempArray, False)
                If BuildInfra = True Then
                    Call WriteData("AirNode", "AirNewCap", NewCapArray, , False)
                End If
            End If


            'repeat for duration
            YearNum += 1

        Loop


    End Sub

    Sub AirInputFiles()

        Dim stratarray(90, 95) As String

        'get nodal external variable file suffix name
        If UpdateExtVars = True Then
            If NewAirCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If

        'get the elasticity values
        Call ReadData("AirNode", "Elasticity", AirEl)

        If TripRates = "Strategy" Then
            'read from the strategy file
            Call ReadData("Strategy", "", stratarray)
            For r = 1 To 90
                AirTripRates(r) = stratarray(r, 94)
            Next
        End If

    End Sub


    Sub GetAirportInputData()

        Dim AirportField As Integer

        AirportCount = 1

        Do Until AirportCount > MaxAirNode

            If YearNum = 1 Then
                'if it is initial year, read from the initial input
                AirportField = 1
                'loop through all elements of the input line, transferring the data to the airport base data array
                Do While AirportField < 14
                    AirportBaseData(AirportCount, AirportField) = NodeInputArray(AirportCount, AirportField)
                    AirportField += 1
                Loop
                AirpTripsLatent(AirportCount) = 0
            Else
                'if it is not initial year, read from the temp input format
                AirportField = 1
                'loop through all elements of the input line, transferring the data to the airport base data array
                Do While AirportField < 14
                    AirportBaseData(AirportCount, AirportField) = NodeInputArray(AirportCount, AirportField)
                    AirportField += 1
                Loop
                AirpTripsLatent(AirportCount) = NodeInputArray(AirportCount, 14)
            End If

            AirportCount += 1
        Loop

    End Sub

    Sub GetAirportExtVar()

        Dim rowcount As Integer
        Dim planechange As Boolean

        'get external variables for this year
        Call ReadData("AirNode", "ExtVar", AirportExtVar, , YearNum)

        'set rowcount to 1
        rowcount = 1

        'add in value
        Do Until rowcount > MaxAirNode
            'add in built capacity
            AirportExtVar(rowcount, 5) += AirpAddedCap(rowcount, 0)
            AirportExtVar(rowcount, 6) += AirpAddedCap(rowcount, 1)
            '1009Change check if capacity is greater than in the previous year, and if so then set the various capacity constraint checkers to false
            If AirportExtVar(rowcount, 5) > AirportBaseData(rowcount, 4) Then
                AirpOldConstraint(rowcount) = False
                AirpTermCapCheck(rowcount) = False
                For f = 1 To 223
                    If AirFlowBaseData(f, 1) = rowcount Then
                        AirfCapConst(f, 0) = False
                    ElseIf AirFlowBaseData(f, 2) = rowcount Then
                        AirfCapConst(f, 1) = False
                    End If
                Next
            End If
            If AirportExtVar(rowcount, 6) > AirportBaseData(rowcount, 5) Then
                AirpOldConstraint(rowcount) = False
                AirpRunCapCheck(rowcount) = False
                For f = 1 To 223
                    If AirFlowBaseData(f, 1) = rowcount Then
                        AirfCapConst(f, 0) = False
                    ElseIf AirFlowBaseData(f, 2) = rowcount Then
                        AirfCapConst(f, 1) = False
                    End If
                Next
            End If
            planechange = False
            '1009Changev2 if plane sizes or load factors have changed then also need to set capacity constraint checkers to false
            If AirportExtVar(rowcount, 7) <> AirportBaseData(rowcount, 9) Then
                planechange = True
            End If
            If AirportExtVar(rowcount, 8) <> AirportBaseData(rowcount, 10) Then
                planechange = True
            End If
            If AirportExtVar(rowcount, 9) <> AirportBaseData(rowcount, 11) Then
                planechange = True
            End If
            If AirportExtVar(rowcount, 10) <> AirportBaseData(rowcount, 12) Then
                planechange = True
            End If
            If planechange = True Then
                'check if a runway capacity constraint applies, if so then set the capacity constraint checkers to false - but don't do this if it was a terminal constrant
                If AirpRunCapCheck(rowcount) = True Then
                    AirpOldConstraint(rowcount) = False
                    AirpRunCapCheck(rowcount) = False
                    For f = 1 To 223
                        If AirFlowBaseData(f, 1) = rowcount Then
                            AirfCapConst(f, 0) = False
                        ElseIf AirFlowBaseData(f, 2) = rowcount Then
                            AirfCapConst(f, 1) = False
                        End If
                    Next
                End If
            End If
            rowcount += 1
        Loop

    End Sub

    Sub AirNodeChange()

        Dim aircount As Integer

        aircount = 1

        'loop through airports calculating new total international passengers
        Do Until aircount > MaxAirNode

            'calculate values for each ratio in the model
            'now includes option to use variable elasticities
            If VariableEl = True Then
                OldX = AirportBaseData(aircount, 3)
                'pop ratio
                OldY = AirportBaseData(aircount, 6)
                If TripRates = "Strategy" Then
                    NewY = AirportExtVar(aircount, 2) * AirTripRates(YearNum)
                Else
                    NewY = AirportExtVar(aircount, 2)
                End If

                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = AirEl(YearNum, 4)
                    Call VarElCalc()
                    AirpPopRat = VarRat
                Else
                    If TripRates = "Strategy" Then
                        AirpPopRat = ((AirportExtVar(aircount, 2) * AirTripRates(YearNum)) / AirportBaseData(aircount, 6)) ^ AirEl(YearNum, 4)
                    Else
                        AirpPopRat = (AirportExtVar(aircount, 2) / AirportBaseData(aircount, 6)) ^ AirEl(YearNum, 4)
                    End If

                End If
                'gva ratio
                OldY = AirportBaseData(aircount, 7)
                NewY = AirportExtVar(aircount, 3)
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = AirEl(YearNum, 5)
                    Call VarElCalc()
                    AirpGvaRat = VarRat
                Else
                    AirpGvaRat = (AirportExtVar(aircount, 3) / AirportBaseData(aircount, 7)) ^ AirEl(YearNum, 5)
                End If
                'cost ratio
                OldY = AirportBaseData(aircount, 8)
                NewY = AirportExtVar(aircount, 4) + AirpCapU(aircount, 3)
                If OldY > 0 Then
                    If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                        OldEl = AirEl(YearNum, 6)
                        Call VarElCalc()
                        AirpCostRat = VarRat
                    Else
                        AirpCostRat = ((AirportExtVar(aircount, 4) + AirpCapU(aircount, 3)) / AirportBaseData(aircount, 8)) ^ AirEl(YearNum, 6)
                    End If
                Else
                    AirpCostRat = 1
                End If

            Else
                If TripRates = "Strategy" Then
                    AirpPopRat = ((AirportExtVar(aircount, 2) * AirTripRates(YearNum)) / AirportBaseData(aircount, 6)) ^ AirEl(YearNum, 4)
                Else
                    AirpPopRat = (AirportExtVar(aircount, 2) / AirportBaseData(aircount, 6)) ^ AirEl(YearNum, 4)
                End If
                AirpGvaRat = (AirportExtVar(aircount, 3) / AirportBaseData(aircount, 7)) ^ AirEl(YearNum, 5)
                AirpCostRat = ((AirportExtVar(aircount, 4) + AirpCapU(aircount, 3)) / AirportBaseData(aircount, 8)) ^ AirEl(YearNum, 6)
            End If

            AirpTripRat = AirpPopRat * AirpGvaRat * AirpCostRat

            '1009Change only update actual trips if demand is not already at the constraint
            If AirpTermCapCheck(aircount) = False Then
                If AirpRunCapCheck(aircount) = False Then
                    'estimate the new unconstrained actual and latent demand for trips
                    AirpTripsNew(aircount) = AirportBaseData(aircount, 3) * AirpTripRat
                    AirpTripsLatent(aircount) = AirpTripsLatent(aircount) * AirpTripRat
                Else
                    'otherwise just estimate the latent demand based on growth in both previous latent demand and actual capped demand
                    AirpTripsLatent(aircount) = (AirpTripsLatent(aircount) * AirpTripRat) + (AirportBaseData(aircount, 3) * (AirpTripRat - 1))
                    AirpTripsNew(aircount) = AirportBaseData(aircount, 3)
                End If
            Else
                'otherwise just estimate the latent demand based on growth in both previous latent demand and actual capped demand
                AirpTripsLatent(aircount) = (AirpTripsLatent(aircount) * AirpTripRat) + (AirportBaseData(aircount, 3) * (AirpTripRat - 1))
                AirpTripsNew(aircount) = AirportBaseData(aircount, 3)
            End If
            '1009Change end

            aircount += 1

        Loop

    End Sub

    Sub GetAirFlowInputData()

        Dim AirportField As Integer

        AirportCount = 1

        Do Until AirportCount > MaxAirFlow
            If YearNum = 1 Then
                'if it is initial year, read from the initial input
                AirportField = 0
                'loop through all elements of the input line, transferring the data to the airport base data array
                Do While AirportField < 10
                    AirFlowBaseData(AirportCount, AirportField) = FlowInputArray(AirportCount, AirportField)
                    AirportField += 1
                Loop

                AirfTripsLatent(AirportCount) = 0

                'set all the capacity constraint checks for the flow 
                AirfCapConst(AirportCount, 0) = False
                AirfCapConst(AirportCount, 1) = False

            Else
            'if it is not initial year, read from the temp input
            AirportField = 0
            'loop through all elements of the input line, transferring the data to the airport base data array
            Do While AirportField < 10
                AirFlowBaseData(AirportCount, AirportField) = FlowInputArray(AirportCount, AirportField)
                AirportField += 1
            Loop

            AirfTripsLatent(AirportCount) = FlowInputArray(AirportCount, 10)

            'set all the capacity constraint checks for the flow 
            If FlowInputArray(AirportCount, 11) = 0 Then
                AirfCapConst(AirportCount, 0) = False
            ElseIf FlowInputArray(AirportCount, 11) = 1 Then
                AirfCapConst(AirportCount, 0) = True
            End If
            If FlowInputArray(AirportCount, 12) = 0 Then
                AirfCapConst(AirportCount, 1) = False
            ElseIf FlowInputArray(AirportCount, 12) = 1 Then
                AirfCapConst(AirportCount, 1) = True
            End If

            End If

            AirportCount += 1
        Loop

    End Sub

    Sub GetAirFlowExtVar()

        'get external variables for this year
        Call ReadData("AirFlow", "ExtVar", AirFlowExtVar, , YearNum)

    End Sub

    Sub AirFlowChange()

        Dim aircount As Integer
        Dim oairport, dairport As Integer
        Dim ocharge, dcharge As Double

        aircount = 1

        'loop through flows calculating new passenger numbers
        Do Until aircount > MaxAirFlow
            oairport = AirFlowBaseData(aircount, 1)
            dairport = AirFlowBaseData(aircount, 2)
            If oairport < 29 Then
                ocharge = 0.5 * AirpCapU(oairport, 3)
            Else
                ocharge = 0
            End If
            If dairport < 29 Then
                dcharge = 0.5 * AirpCapU(dairport, 3)
            Else
                dcharge = 0
            End If
            FlowCharge(aircount) = ocharge + dcharge

            'estimate the unconstrained growth in demand for both the actual demand and the latent demand
            'calculate values for each ratio in the model
            'now includes option for variable elasticities
            If VariableEl = True Then
                OldX = AirFlowBaseData(aircount, 3)
                'v1.3 mod if there were no trips previously then variable elasticities won't work, so use standard (output will still be zero trips anyway)
                If OldX > 0 Then
                    'pop ratio
                    OldY = AirFlowBaseData(aircount, 4) + AirFlowBaseData(aircount, 5)
                    If TripRates = "Strategy" Then
                        NewY = (CDbl(AirFlowExtVar(aircount, 2)) + CDbl(AirFlowExtVar(aircount, 3))) * AirTripRates(YearNum)
                    Else
                        NewY = AirFlowExtVar(aircount, 2) + AirFlowExtVar(aircount, 3)
                    End If

                    If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                        OldEl = AirEl(YearNum, 1)
                        Call VarElCalc()
                        AirfPopRat = VarRat
                    Else
                        AirfPopRat = ((CDbl(AirFlowExtVar(aircount, 2)) + CDbl(AirFlowExtVar(aircount, 3))) / (AirFlowBaseData(aircount, 4) + AirFlowBaseData(aircount, 5))) ^ AirEl(YearNum, 1)
                    End If
                    'gva ratio
                    OldY = AirFlowBaseData(aircount, 6) + AirFlowBaseData(aircount, 7)
                    NewY = CDbl(AirFlowExtVar(aircount, 4)) + CDbl(AirFlowExtVar(aircount, 5))
                    If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                        OldEl = AirEl(YearNum, 2)
                        Call VarElCalc()
                        AirfGvaRat = VarRat
                    Else
                        AirfGvaRat = ((CDbl(AirFlowExtVar(aircount, 4)) + CDbl(AirFlowExtVar(aircount, 5))) / (AirFlowBaseData(aircount, 6) + AirFlowBaseData(aircount, 7))) ^ AirEl(YearNum, 2)
                    End If

                    'cost ratio
                    OldY = AirFlowBaseData(aircount, 8)
                    NewY = CDbl(AirFlowExtVar(aircount, 6)) + FlowCharge(aircount)
                    If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                        OldEl = AirEl(YearNum, 3)
                        Call VarElCalc()
                        AirfCostRat = VarRat
                    Else
                        AirfCostRat = ((CDbl(AirFlowExtVar(aircount, 6)) + FlowCharge(aircount)) / AirFlowBaseData(aircount, 8)) ^ AirEl(YearNum, 3)
                    End If
                Else
                    If TripRates = "Strategy" Then
                        AirfPopRat = (((CDbl(AirFlowExtVar(aircount, 2)) + CDbl(AirFlowExtVar(aircount, 3))) * AirTripRates(YearNum)) / (AirFlowBaseData(aircount, 4) + AirFlowBaseData(aircount, 5))) ^ AirEl(YearNum, 1)
                    Else
                        AirfPopRat = ((CDbl(AirFlowExtVar(aircount, 2)) + CDbl(AirFlowExtVar(aircount, 3))) / (AirFlowBaseData(aircount, 4) + AirFlowBaseData(aircount, 5))) ^ AirEl(YearNum, 1)
                    End If
                    AirfGvaRat = ((CDbl(AirFlowExtVar(aircount, 4)) + CDbl(AirFlowExtVar(aircount, 5))) / (AirFlowBaseData(aircount, 6) + AirFlowBaseData(aircount, 7))) ^ AirEl(YearNum, 2)
                    AirfCostRat = ((CDbl(AirFlowExtVar(aircount, 6)) + FlowCharge(aircount)) / AirFlowBaseData(aircount, 8)) ^ AirEl(YearNum, 3)
                End If
            Else
                If TripRates = "Strategy" Then
                    AirfPopRat = (((CDbl(AirFlowExtVar(aircount, 2)) + CDbl(AirFlowExtVar(aircount, 3)))) * AirTripRates(YearNum)) / (AirFlowBaseData(aircount, 4) + AirFlowBaseData(aircount, 5)) ^ AirEl(YearNum, 1)
                Else
                    AirfPopRat = ((CDbl(AirFlowExtVar(aircount, 2)) + CDbl(AirFlowExtVar(aircount, 3))) / (AirFlowBaseData(aircount, 4) + AirFlowBaseData(aircount, 5))) ^ AirEl(YearNum, 1)
                End If
                AirfGvaRat = ((CDbl(AirFlowExtVar(aircount, 4)) + CDbl(AirFlowExtVar(aircount, 5))) / (AirFlowBaseData(aircount, 6) + AirFlowBaseData(aircount, 7))) ^ AirEl(YearNum, 2)
                AirfCostRat = ((CDbl(AirFlowExtVar(aircount, 6)) + FlowCharge(aircount)) / AirFlowBaseData(aircount, 8)) ^ AirEl(YearNum, 3)
            End If

            'use the ratios to estimate the new actual and latent level of trips on the flow

            AirfTripRat = AirfPopRat * AirfGvaRat * AirfCostRat

            '1009Change only update actual trips if demand is not already at the constraint
            If AirfCapConst(aircount, 0) = False Then
                If AirfCapConst(aircount, 1) = False Then
                    AirfTripsNew(aircount) = AirFlowBaseData(aircount, 3) * AirfTripRat
                    AirfTripsLatent(aircount) = AirfTripsLatent(aircount) * AirfTripRat
                Else
                    AirfTripsLatent(aircount) = (AirfTripsLatent(aircount) * AirfTripRat) + (AirFlowBaseData(aircount, 3) * (AirfTripRat - 1))
                    AirfTripsNew(aircount) = AirFlowBaseData(aircount, 3)
                End If
            Else
                AirfTripsLatent(aircount) = (AirfTripsLatent(aircount) * AirfTripRat) + (AirFlowBaseData(aircount, 3) * (AirfTripRat - 1))
                AirfTripsNew(aircount) = AirFlowBaseData(aircount, 3)
            End If
            '1009Change End

            aircount += 1
        Loop

        'update the airport domestic trip arrays
        Call UpdateDomesticTrips()

    End Sub

    Sub VarElCalc()
        Dim alpha, beta As Double
        Dim xnew As Double

        alpha = OldX / Math.Exp(OldEl)
        beta = (Math.Log(OldX / alpha)) / OldY
        xnew = alpha * Math.Exp(beta * NewY)
        VarRat = xnew / OldX

    End Sub

    Sub AirConstraints()
        'this checks if growth in the year being modelled has led to capacity constraints being reached, and if so applies the constraints

        Dim aircount As Integer
        Dim termrat As Double
        Dim flownum As Long
        Dim domflights As Double
        Dim intflights As Double
        Dim allflights As Double
        Dim flightrat As Double
        Dim airpdomscaledtrips As Double
        Dim airpdomtriprat As Double
        Dim latenttrips As Double
        Dim latentdomtrips As Double
        Dim constcheck As Boolean
        Dim latentrat As Double
        Dim constflowstring As String
        Dim extratrips As Double
        Dim flowfull(223) As Boolean
        Dim constcount As Integer
        Dim constarray() As String
        Dim addtrips As Double

        aircount = 1

        For f = 1 To 223
            flowfull(f) = False
        Next

        'apply the terminal capacity constraint
        'set constraint checker to false
        constcheck = False
        Do While aircount < 29

            'sum international and domestic traffic
            AirpPass(aircount) = AirpDomTripsNew(aircount) + AirpTripsNew(aircount)

            '1009Change don't need to do this if a constraint exists from a previous year
            If AirpOldConstraint(aircount) = False Then
                'get maximum terminal capacity value
                AirpTermCap(aircount) = AirportExtVar(aircount, 5)
                'check if predicted passenger numbers exceed capacity
                If AirpPass(aircount) < AirpTermCap(aircount) Then
                    'if they don't, then...
                    '1308 change this section revised
                    'set the temporary latent trips total to zero
                    latenttrips = 0
                    constflowstring = ""
                    constcount = -1
                    'check if there is any latent international demand and add it to the temporary latent trips total
                    If AirpTripsLatent(aircount) > 0 Then
                        'if so then add this to the temporary latent trips total 
                        latenttrips = AirpTripsLatent(aircount)
                    End If
                    'check if there is any latent domestic demand and add it to the temporary latent trips total
                    If AirpDomTripsLatent(aircount) > 0 Then
                        'if so then need to check whether this has been constrained at the origin or the destination end
                        'loop through all flows
                        flownum = 1
                        Do Until flownum > MaxAirFlow
                            If flowfull(flownum) = False Then
                                'check if origin airport is the one we're currently modelling
                                If AirFlowBaseData(flownum, 1) = aircount Then
                                    'if so then check if a capacity constraint applies at the origin airport
                                    If AirfCapConst(flownum, 0) = True Then
                                        'if so then add the latent demand to the temporary total
                                        latenttrips = latenttrips + AirfTripsLatent(flownum)
                                        constflowstring = constflowstring & flownum & ","
                                        constcount += 1
                                    End If
                                    'if not then check if the destination airport is the one we're currently modelling
                                ElseIf AirFlowBaseData(flownum, 2) = aircount Then
                                    'if so then check if a capacity constraint applies at the destination airport
                                    If AirfCapConst(flownum, 1) = True Then
                                        'if so then add the latent demand to the temporary total
                                        latenttrips = latenttrips + AirfTripsLatent(flownum)
                                        constflowstring = constflowstring & flownum & ","
                                        constcount += 1
                                    End If
                                End If
                            End If
                            flownum += 1
                        Loop
                    End If
                    'check how much of the latent demand can be accommodated
                    extratrips = AirpTermCap(aircount) - AirpPass(aircount)
                    latentrat = extratrips / latenttrips
                    If latentrat > 1 Then
                        latentrat = 1
                    End If
                    'add extra international trips
                    AirpTripsNew(aircount) += AirpTripsLatent(aircount) * latentrat
                    'adjust latent trips array value
                    AirpTripsLatent(aircount) = AirpTripsLatent(aircount) * (1 - latentrat)
                    'add extra domestic trips
                    If constcount > -1 Then
                        constarray = Split(constflowstring, ",")
                        For f = 0 To constcount
                            addtrips = AirfTripsNew(constarray(f)) * latentrat
                            AirfTripsNew(constarray(f)) += addtrips
                            'adjust domestic latent and total trip arrays
                            AirpDomTripsLatent(aircount) = AirpDomTripsLatent(aircount) - addtrips
                            AirpDomTripsNew(aircount) += addtrips
                            flowfull(constarray(f)) = True
                            'adjust latent trips array value
                            AirfTripsLatent(constarray(f)) = AirfTripsLatent(constarray(f)) * (1 - latentrat)
                        Next
                    End If
                    'add the latent trips to the total trips from the airport
                    AirpPass(aircount) = AirpTripsNew(aircount) + AirpDomTripsNew(aircount)
                    'check if predicted passenger numbers now exceed capacity
                    If AirpPass(aircount) > AirpTermCap(aircount) Then
                        'if they do then set constraint checker to true
                        constcheck = True
                        'if they don't then set constraint checker to false and move on to runway capacity constraint
                    Else
                        constcheck = False
                    End If
                Else
                    constcheck = True
                    AirpOldConstraint(aircount) = True
                End If

                'If a terminal capacity constraint exists then adjust passenger numbers accordingly
                If constcheck = True Then
                    'if they do, then set constraint checker to true, and need to apply constraint
                    AirpTermCapCheck(aircount) = True
                    'apply terminal constraint by scaling number of passengers on each relevant flow/airport total so that they sum to give the maximum capacity
                    'calculate scaling factor
                    termrat = AirpPass(aircount) / AirpTermCap(aircount)
                    'set temporary variable to equal unconstrained trips
                    latenttrips = AirpTripsNew(aircount)
                    'constrain the number of international passengers
                    AirpTripsNew(aircount) = AirpTripsNew(aircount) / termrat
                    'estimate the number of suppressed trips and add to the latent demand array
                    latenttrips = latenttrips - AirpTripsNew(aircount)
                    AirpTripsLatent(aircount) = AirpTripsLatent(aircount) + latenttrips
                    'set temporary domestic variable to equal unconstrained number of domestictrips
                    latentdomtrips = AirpDomTripsNew(aircount)
                    'loop through all flows, checking if the airport is the origin or destination, and scaling if so
                    flownum = 1
                    Do Until flownum > MaxAirFlow
                        'check if origin airport is the one we're currently scaling
                        If AirFlowBaseData(flownum, 1) = aircount Then
                            '1009Change only do this if a constraint doesn't already apply to the flow
                            If AirfOldConstraint(flownum) = False Then
                                'set temporary variable to equal unconstrained trips
                                latenttrips = AirfTripsNew(flownum)
                                'if so then scale it by the ratio
                                AirfTripsNew(flownum) = AirfTripsNew(flownum) / termrat
                                'estimate the number of suppressed trips and add to the latent demand array
                                latenttrips = latenttrips - AirfTripsNew(flownum)
                                latentdomtrips = latentdomtrips - AirfTripsNew(flownum)
                                AirfTripsLatent(flownum) = AirfTripsLatent(flownum) + latenttrips
                                'change the origin capacity constraint check to true
                                AirfCapConst(flownum, 0) = True
                                AirfOldConstraint(flownum) = True
                            Else
                                'otherwise just add the existing latent trips for this flow to the latent domestic trip total
                                latentdomtrips = latentdomtrips - AirfTripsNew(flownum) + AirfTripsLatent(flownum)
                            End If

                            'if not then check if destination airport is the one we're currently scaling
                        ElseIf AirFlowBaseData(flownum, 2) = aircount Then
                            '1009Change only do this if a constraint doesn't already apply to the flow
                            If AirfOldConstraint(flownum) = False Then
                                'set temporary variable to equal unconstrained trips
                                latenttrips = AirfTripsNew(flownum)
                                AirfTripsNew(flownum) = AirfTripsNew(flownum) / termrat
                                'estimate the number of suppressed trips and add to the latent demand array
                                latenttrips = latenttrips - AirfTripsNew(flownum)
                                latentdomtrips = latentdomtrips - AirfTripsNew(flownum)
                                AirfTripsLatent(flownum) = AirfTripsLatent(flownum) + latenttrips
                                'change the destination capacity constraint check to true
                                AirfCapConst(flownum, 1) = True
                                AirfOldConstraint(flownum) = True
                            Else
                                'otherwise just add the existing latent trips for this flow to the latent domestic trip total
                                latentdomtrips = latentdomtrips - AirfTripsNew(flownum) + AirfTripsLatent(flownum)
                            End If
                        Else
                            'otherwise do nothing and move on to next flow
                        End If
                        flownum += 1
                    Loop
                    'assign value to latent airport domestic trips array
                    AirpDomTripsLatent(aircount) = latentdomtrips
                    'lines added which add the difference between the scaled and unscaled figures to the latent demand arrays
                End If
            End If

            aircount += 1
        Loop

        'recalculate the airport domestic trip arrays based on the terminal constrained flows
        Call UpdateDomesticTrips()

        'apply the runway capacity constraints
        aircount = 1
        constcheck = False
        Do While aircount < 29
            'calculate the terminal constrained number of flights
            'math.ceiling function rounds up to the nearest whole number as can't have fractions of flights
            domflights = Math.Ceiling(AirpDomTripsNew(aircount) / (AirportExtVar(aircount, 7) * (AirportExtVar(aircount, 9) / 100)))
            intflights = Math.Ceiling(AirpTripsNew(aircount) / (AirportExtVar(aircount, 8) * (AirportExtVar(aircount, 10) / 100)))
            allflights = domflights + intflights
            'set value of AirpATM array
            AirpATM(aircount) = allflights
            'check if the terminal constraint is more restrictive than the runway constraint
            'change 30/07/13 this doesn't seem to be happening - if terminal constraint applies then no point applying runway constraint too

            '1009Change don't need to do this if a constraint exists from a previous year
            If AirpOldConstraint(aircount) = False Then
                'check if unconstrained flights exceed runway capacity
                If allflights > AirportExtVar(aircount, 6) Then
                    'if they do, then set constraint checker to true, and then apply constraint
                    AirpRunCapCheck(aircount) = True
                    constcheck = True
                    AirpOldConstraint(aircount) = True
                Else
                    'if they don't, then check if the terminal constraint is more restrictive than the runway constraint
                    If AirpTermCapCheck(aircount) = True Then
                        'if it is then set constraint checker to false (because runway constraint doesn't apply) and skip next section
                        constcheck = False
                    Else
                        '1308 change - this section is irrelevant, because if there is no constraint then latent demand already added, and if there is then don't want to add it
                        constcheck = False
                    End If
                End If

                'If a runway capacity constraint exists then adjust passenger numbers accordingly
                If constcheck = True Then
                    flightrat = AirportExtVar(aircount, 6) / allflights
                    'estimate constrained domestic and international flight numbers, rounded to nearest whole number as can't have fractions of flights
                    domflights = Math.Round(domflights * flightrat)
                    intflights = Math.Round(intflights * flightrat)
                    'update the AirpATM array
                    AirpATM(aircount) = domflights + intflights
                    'scale passenger numbers based on these constrained numbers of flights
                    'set temporary variable to equal unconstrained number of trips
                    latenttrips = AirpTripsNew(aircount)
                    'international trips done by multiplying number of flights by aeroplane size and load factor
                    AirpTripsNew(aircount) = intflights * AirportExtVar(aircount, 8) * (AirportExtVar(aircount, 10) / 100)
                    'estimate the number of suppressed trips and add to the latent demand array
                    latenttrips = latenttrips - AirpTripsNew(aircount)
                    AirpTripsLatent(aircount) = latenttrips

                    'for domestic trips calculate number of passengers first, then loop through flows
                    'set temporary domestic variable to equal unconstrained number of domestictrips
                    latentdomtrips = AirpDomTripsNew(aircount)
                    airpdomscaledtrips = domflights * AirportExtVar(aircount, 7) * (AirportExtVar(aircount, 9) / 100)
                    airpdomtriprat = airpdomscaledtrips / AirpDomTripsNew(aircount)
                    'loop through flows, scaling trip totals where the airport in question is the origin or destination
                    flownum = 1
                    Do Until flownum > MaxAirFlow
                        'first check whether the origin airport is one of those being modelled
                        If AirFlowBaseData(flownum, 1) = aircount Then
                            '1009Change only do this if a constraint doesn't already apply to the flow
                            If AirfOldConstraint(flownum) = False Then
                                'set temporary variable to equal unconstrained flow trips
                                latenttrips = AirfTripsNew(flownum)
                                AirfTripsNew(flownum) = AirfTripsNew(flownum) * airpdomtriprat
                                'estimate the number of suppressed trips and add to the latent demand array
                                latenttrips = latenttrips - AirfTripsNew(flownum)
                                latentdomtrips = latentdomtrips - AirfTripsNew(flownum)
                                AirfTripsLatent(flownum) = AirfTripsLatent(flownum) + latenttrips
                                'change the origin capacity constraint check to true
                                AirfCapConst(flownum, 0) = True
                                AirfOldConstraint(flownum) = True
                            Else
                                'otherwise just add the existing latent trips for this flow to the latent domestic trip total
                                latentdomtrips = latentdomtrips - AirfTripsNew(flownum) + AirfTripsLatent(flownum)
                            End If

                            'if not then check whether the destination airport is one of those being modelled
                        ElseIf AirFlowBaseData(flownum, 2) = aircount Then
                            '1009Change only do this if a constraint doesn't already apply to the flow
                            If AirfOldConstraint(flownum) = False Then
                                'set temporary variable to equal unconstrained flow trips
                                latenttrips = AirfTripsNew(flownum)
                                AirfTripsNew(flownum) = AirfTripsNew(flownum) * airpdomtriprat
                                'estimate the number of suppressed trips and add to the latent demand array
                                latenttrips = latenttrips - AirfTripsNew(flownum)
                                latentdomtrips = latentdomtrips - AirfTripsNew(flownum)
                                AirfTripsLatent(flownum) = AirfTripsLatent(flownum) + latenttrips
                                'change the destination capacity constraint check to true
                                AirfCapConst(flownum, 1) = True
                                AirfOldConstraint(flownum) = True
                            Else
                                'otherwise just add the existing latent trips for this flow to the latent domestic trip total
                                latentdomtrips = latentdomtrips - AirfTripsNew(flownum) + AirfTripsLatent(flownum)
                            End If
                        Else
                            'otherwise move to next flow
                        End If
                        flownum += 1
                    Loop
                    'assign value to latent airport domestic trips array
                    AirpDomTripsLatent(aircount) = latentdomtrips
                    'add the difference between the scaled and unscaled figures to the latent demand arrays - and add a latent airport domestic trips array
                    'finally, recalculate all the domestic trip totals, so that airports on the other end of the constrained flows have the new correct totals
                    If YearNum = 1 Then
                        Dim msg = 3 & "," & AirfTripsNew(1)
                        MsgBox(msg)
                    End If

                    Call UpdateDomesticTrips()
                End If
            End If

            aircount += 1
        Loop

        'recalculate the domestic trip totals yet again, as some airports considered earlier may be affected by constraints at airports considered later
        Call UpdateDomesticTrips()
        '**** could check again if there is scope for adding in some of the latent demand here

        '3007 mod need to recalculate flights here too as otherwise may be basing them on the unconstrained passenger numbers
        For a = 1 To 28
            domflights = Math.Ceiling(AirpDomTripsNew(a) / (AirportExtVar(a, 7) * (AirportExtVar(a, 9) / 100)))
            intflights = Math.Ceiling(AirpTripsNew(a) / (AirportExtVar(a, 8) * (AirportExtVar(a, 10) / 100)))
            allflights = domflights + intflights
            'set value of AirpATM array
            AirpATM(a) = allflights
        Next

        aircount = 1
        Do While aircount < 29
            AirpPass(aircount) = AirpDomTripsNew(aircount) + AirpTripsNew(aircount)
            aircount += 1
        Loop

    End Sub

    Sub UpdateDomesticTrips()
        'this subroutine blanks and recalculates the domestic trip totals for each airport based on the latest set of flow estimates
        Dim airpnum As Integer
        Dim flownum As Long

        'first need to blank the variables
        airpnum = 1
        Do While airpnum < 29
            AirpDomTripsNew(airpnum) = 0
            airpnum += 1
        Loop
        'next add in the new constrained flow totals
        flownum = 1
        Do Until flownum > MaxAirFlow
            'first check whether the origin airport is one of those being modelled
            If AirFlowBaseData(flownum, 1) < 29 Then
                airpnum = AirFlowBaseData(flownum, 1)
                '**debug test
                If airpnum = 14 Then
                    airpnum = 14
                End If
                AirpDomTripsNew(airpnum) += AirfTripsNew(flownum)
            End If
            'then check whether the destination airport is one of those being modelled
            If AirFlowBaseData(flownum, 2) < 29 Then
                airpnum = AirFlowBaseData(flownum, 2)
                '**debug test
                If airpnum = 14 Then
                    airpnum = 14
                End If
                AirpDomTripsNew(airpnum) += AirfTripsNew(flownum)
            End If
            flownum += 1
        Loop


    End Sub

    Sub AirFuelConsumption()

        Dim airpnum As Integer
        Dim flights As Double
        Dim flownum As Long

        'calculate the international fuel consumption for each airport
        airpnum = 1
        Do While airpnum < 29
            'reestimate number of international flights
            flights = Math.Ceiling(AirpTripsNew(airpnum) / (AirportExtVar(airpnum, 8) * (AirportExtVar(airpnum, 10) / 100)))
            'estimate fuel consumption
            'total international fuel consumption equals fuel per seat km * average flow length * mean plane capacity * number of atm
            AirpIntFuel(airpnum) = AirportExtVar(airpnum, 12) * AirportExtVar(airpnum, 11) * AirportExtVar(airpnum, 8) * flights
            'divide total by two because we are assuming half the fuel is supplied abroad
            AirpIntFuel(airpnum) = AirpIntFuel(airpnum) / 2
            'move on to next airport
            airpnum += 1
        Loop

        'calculate the domestic fuel consumption for each flow
        flownum = 1
        Do Until flownum > MaxAirFlow
            'estimate number of flights on the flow
            'get origin airport number
            airpnum = AirFlowBaseData(flownum, 1)
            'if origin airport isn't included in the node database then get destination airport number
            If airpnum > 28 Then
                airpnum = AirFlowBaseData(flownum, 2)
            End If
            flights = Math.Ceiling(AirfTripsNew(flownum) / (AirportExtVar(airpnum, 7) * (AirportExtVar(airpnum, 9) / 100)))
            'estimate fuel consumption
            'total domestic fuel consumption equals fuel per seat km * flow length * mean plane capacity * number of atm
            AirfFuel(flownum) = AirportExtVar(airpnum, 12) * AirFlowBaseData(flownum, 9) * AirportExtVar(airpnum, 7) * flights
            flownum += 1
        Loop

    End Sub

    Sub AirOutputValues()
        Dim aircount As Integer
        Dim flownum As Long
        Dim cuval As Double
        Dim newcapnum As Integer


        'write to output array for node and flow
        aircount = 1
        'loop through all airports writing values to the output files
        Do While aircount < 29
            'concatenate the output row for the node file
            NodeOutputArray(aircount, 0) = YearNum
            NodeOutputArray(aircount, 1) = aircount
            NodeOutputArray(aircount, 2) = AirpPass(aircount)
            NodeOutputArray(aircount, 3) = AirpDomTripsNew(aircount)
            NodeOutputArray(aircount, 4) = AirpTripsNew(aircount)
            NodeOutputArray(aircount, 5) = AirpATM(aircount)
            NodeOutputArray(aircount, 6) = AirpIntFuel(aircount)
            aircount += 1
        Loop
        flownum = 1
        'loop through all flows writing values to the output files
        Do Until flownum > MaxAirFlow
            'concatenate the output row for the flow file
            FlowOutputArray(flownum, 0) = YearNum
            FlowOutputArray(flownum, 1) = AirFlowBaseData(flownum, 0)
            FlowOutputArray(flownum, 2) = AirfTripsNew(flownum)
            FlowOutputArray(flownum, 3) = AirfFuel(flownum)
            flownum += 1
        Loop

        newcapnum = 1
        'update airport base data and write to temp file
        aircount = 1
        'loop through all airports
        Do While aircount < 29
            '**CH7 change this so that all values are updated each year - constraint now longer matters as constrained demand stored in latent array
            AirportBaseData(aircount, 1) = AirpPass(aircount)
            AirportBaseData(aircount, 2) = AirpDomTripsNew(aircount)
            AirportBaseData(aircount, 3) = AirpTripsNew(aircount)
            AirportBaseData(aircount, 4) = AirportExtVar(aircount, 5)
            AirportBaseData(aircount, 5) = AirportExtVar(aircount, 6)
            AirportBaseData(aircount, 8) = AirportExtVar(aircount, 4) + AirpCapU(aircount, 3)
            AirportBaseData(aircount, 9) = AirportExtVar(aircount, 7)
            AirportBaseData(aircount, 10) = AirportExtVar(aircount, 8)
            AirportBaseData(aircount, 11) = AirportExtVar(aircount, 9)
            AirportBaseData(aircount, 12) = AirportExtVar(aircount, 10)
            AirportBaseData(aircount, 6) = AirportExtVar(aircount, 2)
            AirportBaseData(aircount, 7) = AirportExtVar(aircount, 3)
            AirportBaseData(aircount, 13) = AirportExtVar(aircount, 11)
            If BuildInfra = True Then
                cuval = AirpPass(aircount) / AirportBaseData(aircount, 4)
                If cuval >= CUCritValue Then
                    AirportBaseData(aircount, 4) += 20000000
                    AirpAddedCap(aircount, 0) += 20000000
                    'write details to output file
                    NewCapArray(newcapnum, 0) = aircount
                    NewCapArray(newcapnum, 1) = YearNum + 1
                    NewCapArray(newcapnum, 2) = 20000000
                    NewCapArray(newcapnum, 3) = 0
                    newcapnum += 1
                    '1009Change update capacity checkers
                    AirpOldConstraint(aircount) = False
                    AirpTermCapCheck(aircount) = False
                    For f = 1 To 223
                        If AirFlowBaseData(f, 1) = aircount Then
                            AirfCapConst(f, 0) = False
                        ElseIf AirFlowBaseData(f, 2) = aircount Then
                            AirfCapConst(f, 1) = False
                        End If
                    Next
                End If
                cuval = AirpATM(aircount) / AirportBaseData(aircount, 5)
                If cuval >= CUCritValue Then
                    AirportBaseData(aircount, 5) += 200000
                    AirpAddedCap(aircount, 1) += 200000
                    'write details to output file
                    NewCapArray(newcapnum, 0) = aircount
                    NewCapArray(newcapnum, 1) = YearNum + 1
                    NewCapArray(newcapnum, 2) = 0
                    NewCapArray(newcapnum, 3) = 200000
                    newcapnum += 1
                    '1009Change update capacity checkers
                    AirpOldConstraint(aircount) = False
                    AirpRunCapCheck(aircount) = False
                    For f = 1 To 223
                        If AirFlowBaseData(f, 1) = aircount Then
                            AirfCapConst(f, 0) = False
                        ElseIf AirFlowBaseData(f, 2) = aircount Then
                            AirfCapConst(f, 1) = False
                        End If
                    Next
                End If
            End If
            'write to temp file
            NodeTempArray(aircount, 0) = aircount
            For x = 1 To 13
                NodeTempArray(aircount, x) = AirportBaseData(aircount, x)
            Next
            NodeTempArray(aircount, 14) = AirpTripsLatent(aircount)

            aircount += 1
        Loop


        'update air flow base data and write to temp 
        flownum = 1
        Do Until flownum > MaxAirFlow
            '**CH8 change this so that all values are updated each year - constraint now longer matters as constrained demand stored in latent array
            AirFlowBaseData(flownum, 3) = AirfTripsNew(flownum)
            AirFlowBaseData(flownum, 4) = AirFlowExtVar(flownum, 2)
            AirFlowBaseData(flownum, 5) = AirFlowExtVar(flownum, 3)
            AirFlowBaseData(flownum, 6) = AirFlowExtVar(flownum, 4)
            AirFlowBaseData(flownum, 7) = AirFlowExtVar(flownum, 5)
            AirFlowBaseData(flownum, 8) = AirFlowExtVar(flownum, 6) + FlowCharge(aircount)

            FlowTempArray(flownum, 0) = YearNum
            'write to temp
            For x = 0 To 9
                FlowTempArray(flownum, x) = AirFlowBaseData(flownum, x)
            Next

            FlowTempArray(flownum, 10) = AirfTripsLatent(flownum)

            'set all the capacity constraint checks for the flow 
            If AirfCapConst(flownum, 0) = False Then
                FlowTempArray(flownum, 11) = 0
            ElseIf AirfCapConst(flownum, 0) = True Then
                FlowTempArray(flownum, 11) = 1
            End If
            If AirfCapConst(flownum, 1) = False Then
                FlowTempArray(flownum, 12) = 0
            ElseIf AirfCapConst(flownum, 1) = True Then
                FlowTempArray(flownum, 12) = 1
            End If
            flownum += 1
        Loop

    End Sub

    Sub NewBaseAirValues()

        Dim aircount As Integer
        Dim flownum As Long
        Dim cuval As Double
        Dim newcapstring As String

        aircount = 1

        'update airport base data
        'loop through all airports
        Do While aircount < 29
            '**CH7 change this so that all values are updated each year - constraint now longer matters as constrained demand stored in latent array
            AirportBaseData(aircount, 1) = AirpPass(aircount)
            AirportBaseData(aircount, 2) = AirpDomTripsNew(aircount)
            AirportBaseData(aircount, 3) = AirpTripsNew(aircount)
            AirportBaseData(aircount, 4) = AirportExtVar(aircount, 5)
            AirportBaseData(aircount, 5) = AirportExtVar(aircount, 6)
            AirportBaseData(aircount, 8) = AirportExtVar(aircount, 4) + AirpCapU(aircount, 3)
            AirportBaseData(aircount, 9) = AirportExtVar(aircount, 7)
            AirportBaseData(aircount, 10) = AirportExtVar(aircount, 8)
            AirportBaseData(aircount, 11) = AirportExtVar(aircount, 9)
            AirportBaseData(aircount, 12) = AirportExtVar(aircount, 10)
            AirportBaseData(aircount, 6) = AirportExtVar(aircount, 2)
            AirportBaseData(aircount, 7) = AirportExtVar(aircount, 3)
            AirportBaseData(aircount, 13) = AirportExtVar(aircount, 11)
            If BuildInfra = True Then
                cuval = AirpPass(aircount) / AirportBaseData(aircount, 4)
                If cuval >= CUCritValue Then
                    AirportBaseData(aircount, 4) += 20000000
                    AirpAddedCap(aircount, 0) += 20000000
                    'write details to output file
                    newcapstring = aircount & "," & (YearNum + 1) & ",20000000,0"
                    anc.WriteLine(newcapstring)
                    '1009Change update capacity checkers
                    AirpOldConstraint(aircount) = False
                    AirpTermCapCheck(aircount) = False
                    For f = 1 To 223
                        If AirFlowBaseData(f, 1) = aircount Then
                            AirfCapConst(f, 0) = False
                        ElseIf AirFlowBaseData(f, 2) = aircount Then
                            AirfCapConst(f, 1) = False
                        End If
                    Next
                End If
                cuval = AirpATM(aircount) / AirportBaseData(aircount, 5)
                If cuval >= CUCritValue Then
                    AirportBaseData(aircount, 5) += 200000
                    AirpAddedCap(aircount, 1) += 200000
                    'write details to output file
                    newcapstring = aircount & "," & (YearNum + 1) & ",0,200000"
                    anc.WriteLine(newcapstring)
                    '1009Change update capacity checkers
                    AirpOldConstraint(aircount) = False
                    AirpRunCapCheck(aircount) = False
                    For f = 1 To 223
                        If AirFlowBaseData(f, 1) = aircount Then
                            AirfCapConst(f, 0) = False
                        ElseIf AirFlowBaseData(f, 2) = aircount Then
                            AirfCapConst(f, 1) = False
                        End If
                    Next
                End If
            End If

            aircount += 1
        Loop

        flownum = 1

        'update air flow base data
        Do Until flownum > MaxAirFlow
            '**CH8 change this so that all values are updated each year - constraint now longer matters as constrained demand stored in latent array
            AirFlowBaseData(flownum, 3) = AirfTripsNew(flownum)
            AirFlowBaseData(flownum, 4) = AirFlowExtVar(flownum, 2)
            AirFlowBaseData(flownum, 5) = AirFlowExtVar(flownum, 3)
            AirFlowBaseData(flownum, 6) = AirFlowExtVar(flownum, 4)
            AirFlowBaseData(flownum, 7) = AirFlowExtVar(flownum, 5)
            AirFlowBaseData(flownum, 8) = AirFlowExtVar(flownum, 6) + FlowCharge(aircount)

            flownum += 1
        Loop
    End Sub

    Sub CalcAirpCapU()
        'airport capacity utilisation array contains terminal CU as index 1 and runway CU as index 2, and the capacity charge as index 3
        'only do this if not in the first year - we assume that no charge is applied in the first year (because it's difficult to calculate flight numbers in this year)
        If YearNum > 1 Then
            'check it is after the application of the charge
            If YearNum >= AirChargeYear Then
                For x = 1 To 28
                    AirpCapU(x, 1) = AirportBaseData(x, 1) / AirportBaseData(x, 4)
                    AirpCapU(x, 2) = AirpATM(x) / AirportBaseData(x, 5)
                    'calculate capacity charges if we are using them
                    If AirCCharge = True Then
                        If AirpCapU(x, 1) > AirpCapU(x, 2) Then
                            If AirpCapU(x, 1) >= 0.25 Then
                                AirpCapU(x, 3) = AirpNonFuelCost(x) * AirChargePer * (AirpCapU(x, 1) ^ 2)
                            Else
                                AirpCapU(x, 3) = 0
                            End If
                        Else
                            If AirpCapU(x, 2) >= 0.25 Then
                                AirpCapU(x, 3) = AirpNonFuelCost(x) * AirChargePer * (AirpCapU(x, 2) ^ 2)
                            Else
                                AirpCapU(x, 3) = 0
                            End If
                        End If
                    Else
                        AirpCapU(x, 3) = 0
                    End If
                Next
            Else
                For x = 1 To 28
                    AirpCapU(x, 3) = 0
                Next
            End If
        Else
            'if it is the first year then calculate the airport fixed costs
            '**note that if these are to be changed at any point in the model run then need to modify this procedure
            For x = 1 To 28
                AirpNonFuelCost(x) = 0.29 * AirportBaseData(x, 8)
            Next
        End If
    End Sub

    Sub ReadAirInput()

        AirportCount = 1

        'read Air node data
        Do Until AirportCount > MaxAirNode
            'read initial input file if year 1, else use updated data
            If YearNum = 1 Then
                'read node input line
                NodeInput = an.ReadLine
                'split line into array
                NodeDetails = Split(NodeInput, ",")
                For x = 1 To 13
                    NodeInputArray(AirportCount, x) = NodeDetails(x)
                Next
                'set latent trips for each airport to zero
                AirpTripsLatent(AirportCount) = 0

                NodeInputArray(AirportCount, 14) = AirpTripsLatent(AirportCount)
            Else
                For x = 1 To 14
                    NodeInputArray(AirportCount, x) = NodeTempArray(AirportCount, x)
                Next

            End If
            AirportCount += 1
        Loop

        AirportCount = 1
        'read Air flow data
        Do Until AirportCount > MaxAirFlow
            'read initial input file if year 1, else use updated data
            If YearNum = 1 Then
                'read node input line
                FlowInput = af.ReadLine
                'split line into array
                FlowDetails = Split(FlowInput, ",")
                For x = 0 To 9
                    FlowInputArray(AirportCount, x) = FlowDetails(x)
                Next
                'set the latent trips value for the flow to zero
                AirfTripsLatent(AirportCount) = 0
                'set all the capacity constraint checks for the flow to zero
                AirfCapConst(AirportCount, 0) = False
                AirfCapConst(AirportCount, 1) = False

                FlowInputArray(AirportCount, 10) = AirfTripsLatent(AirportCount)
                FlowInputArray(AirportCount, 11) = 0
                FlowInputArray(AirportCount, 12) = 0
            Else
                For x = 0 To 12
                    FlowInputArray(AirportCount, x) = FlowTempArray(AirportCount, x)
                Next
            End If
            AirportCount += 1
        Loop

    End Sub


End Module
