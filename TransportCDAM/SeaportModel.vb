﻿Module SeaportModel
    'this version completely revised to forecast five types of freight traffic - capable of dealing with capacity constraints and enhancements on a case by case basis if data available
    'it now also allows variable elasticities over time and by freight type
    'it now also includes a basic fuel consumption estimator
    'now also has the option to build additional infrastructure if capacity exceeds a certain level
    'now also has the option to use variable elasticities
    'now also includes changing fuel efficiency in fuel consumption calculations
    'now includes variable trip rate option
    'v1.7 now corporate with Database function, read/write are using the function in database interface
    'now all file related functions are using databaseinterface

    Dim InputRow As String
    Dim YearNum As Long
    Dim PortDetails() As String
    Dim PortID As Long
    Dim FreightType As Integer
    Dim BaseFreight(5) As Double 'stores base traffic in this order: liquid bulk(1), dry bulk(2), general cargo(3), lolo(4), roro(5)
    Dim BaseCap(5) As Double 'stores base capacity in the same order
    Dim LBCap, DBCap, GCCap, LLCap, RRCap As Double
    Dim PopBase As Double
    Dim GVABase As Double
    Dim CostBase As Double
    Dim PortExtVar(47, 10) As String
    Dim LatentFreight(5) As Double 'latent freight demand in the same order
    Dim PortPopRat As Double
    Dim PortGVARat As Double
    Dim PortCostRat As Double
    Dim FreightRat As Double
    Dim NewFreight(47, 5) As Double
    Dim SeaEl(90, 15) As String
    Dim NewGasOil, NewFuelOil As Double
    Dim AddedCap(5) As Long
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim SeaTripRates(90) As Double
    Dim InputCount As Integer
    Dim NewCapArray(47, 8) As String
    Dim InputArray(47, 15) As String
    Dim OutputArray(48, 9) As String
    Dim TempArray(48, 12) As String
    Dim NewCapNum As Integer


    Public Sub SeaMain()

        'get input files and create output files
        Call SeaInputFiles()

        'start from the entered year
        YearNum = StartYear
        Do Until YearNum > StartYear + Duration

            'get external variables for this port this year
            Call ReadData("Seaport", "ExtVar", PortExtVar, modelRunID, , YearNum)

            'read from initial file if year 1, otherwise update from temp file
            If YearNum = 1 Then
                Call ReadData("Seaport", "Input", InputArray, modelRunID, True)
            Else
                Call ReadData("Seaport", "Input", InputArray, modelRunID, False)
            End If


            InputCount = 1

            Do While InputCount < 48

                'set the newcaparray to the first line to start with
                NewCapNum = 1
                'update the input variables
                Call LoadPortInput()
                'set latent traffic levels to zero
                Call ResetPortLatent()
                'calculate new traffic level, checking for capacity constraint
                Call NewSeaFreightCalc()
                'estimate fuel consumption
                Call SeaFuelConsumption()
                'write to output file
                Call WritePortOutput()

                InputCount += 1
            Loop

            'create file is true if it is the initial year and write to outputfile and temp file
            If YearNum = StartYear Then
                Call WriteData("Seaport", "Output", OutputArray, , True)
                Call WriteData("Seaport", "Temp", TempArray, , True)
                'if the model is building capacity then create new capacity file
                If BuildInfra = True Then
                    Call WriteData("Seaport", "SeaNewCap", NewCapArray, , True)
                End If
            Else
                Call WriteData("Seaport", "Output", OutputArray, , False)
                Call WriteData("Seaport", "Temp", TempArray, , False)
                If BuildInfra = True Then
                    Call WriteData("Seaport", "SeaNewCap", NewCapArray, , False)
                End If
            End If

            'move on to next year
            YearNum += 1

        Loop

    End Sub

    Sub SeaInputFiles()
        Dim stratarray(90, 95) As String

        'get external variables file
        'check if updated version being used
        If UpdateExtVars = True Then
            If NewSeaCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If

        If TripRates = "SubStrategy" Then
            'get the strat values
            Call ReadData("SubStrategy", "", stratarray, modelRunID)
            For r = 1 To 90
                SeaTripRates(r) = stratarray(r, 95)
            Next
        End If

        'get the elasticity values
        Call ReadData("Seaport", "Elasticity", SeaEl, modelRunID)

    End Sub

    Sub LoadPortInput()

        'read initial input file if year 1
        If YearNum = 1 Then
            PortID = InputArray(InputCount, 4)
            BaseFreight(1) = InputArray(InputCount, 5)
            BaseFreight(2) = InputArray(InputCount, 6)
            BaseFreight(3) = InputArray(InputCount, 7)
            BaseFreight(4) = InputArray(InputCount, 8)
            BaseFreight(5) = InputArray(InputCount, 9)
            BaseCap(1) = InputArray(InputCount, 10)
            BaseCap(2) = InputArray(InputCount, 11)
            BaseCap(3) = InputArray(InputCount, 12)
            BaseCap(4) = InputArray(InputCount, 13)
            BaseCap(5) = InputArray(InputCount, 14)
            PopBase = get_population_data_by_economics_scenario_tr_zone(ScenarioID, modelRunYear, "sea", InputCount)
            GVABase = get_regional_gva_data_by_economics_scenario_tr_zone(ScenarioID, modelRunYear, "sea", InputCount)

            CostBase = InputArray(InputCount, 15)
            For x = 1 To 5
                AddedCap(x) = 0
            Next
        Else
            'if not year 1, use updated data
            PortID = InputArray(InputCount, 3)
            BaseFreight(1) = InputArray(InputCount, 4)
            BaseFreight(2) = InputArray(InputCount, 5)
            BaseFreight(3) = InputArray(InputCount, 6)
            BaseFreight(4) = InputArray(InputCount, 7)
            BaseFreight(5) = InputArray(InputCount, 8)
            'BaseCap(1) = InputArray(InputCount, 6)
            'BaseCap(2) = InputArray(InputCount, 7)
            'BaseCap(3) = InputArray(InputCount, 8)
            'BaseCap(4) = InputArray(InputCount, 9)
            'BaseCap(5) = InputArray(InputCount, 10)
            PopBase = get_population_data_by_economics_scenario_tr_zone(ScenarioID, modelRunYear, "sea", InputCount)
            GVABase = get_regional_gva_data_by_economics_scenario_tr_zone(ScenarioID, modelRunYear, "sea", InputCount)
            'needs to create a new function to get cost from the external variable from previous year
            CostBase = get_regional_gva_data_by_economics_scenario_tr_zone(ScenarioID, modelRunYear, "sea", InputCount)
            For x = 1 To 5
                AddedCap(x) = InputArray(InputCount, 8 + x)
            Next

        End If

    End Sub


    Sub ResetPortLatent()
        FreightType = 1
        Do While FreightType < 6
            LatentFreight(FreightType) = 0
            FreightType += 1
        Loop

    End Sub

    Sub NewSeaFreightCalc()
        Dim evindex As Integer
        Dim portcap As Double
        Dim elindex As Integer
        Dim item As Integer

        'add 
        If BuildInfra = True Then
            item = 4
            Do While item < 9
                PortExtVar(PortID, item) += AddedCap(item - 1)
                item += 1
            Loop
        End If

        'loop through all the freight types calculating new freight volumes
        FreightType = 1
        Do While FreightType < 6
            evindex = FreightType + 3
            elindex = (FreightType - 1) * 3
            'calculate ratios - now includes option to use variable elasticities
            If VariableEl = True Then
                OldX = BaseFreight(FreightType)
                'pop ratio
                OldY = PopBase
                If TripRates = "SubStrategy" Then
                    NewY = PortExtVar(PortID, 9) * SeaTripRates(YearNum)
                Else
                    NewY = PortExtVar(PortID, 9)
                End If
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(YearNum, 1 + elindex)
                    Call VarElCalc()
                    PortPopRat = VarRat
                Else
                    If TripRates = "SubStrategy" Then
                        PortPopRat = ((PortExtVar(PortID, 9) * SeaTripRates(YearNum)) / PopBase) ^ SeaEl(YearNum, 1 + elindex)
                    Else
                        PortPopRat = (PortExtVar(PortID, 7) / PopBase) ^ SeaEl(YearNum, 1 + elindex)
                    End If
                End If
                'gva ratio
                OldY = GVABase
                NewY = PortExtVar(PortID, 10)
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(YearNum, 2 + elindex)
                    Call VarElCalc()
                    PortGVARat = VarRat
                Else
                    PortGVARat = (PortExtVar(PortID, 10) / GVABase) ^ SeaEl(YearNum, 2 + elindex)
                End If
                'cost ratio
                OldY = CostBase
                NewY = PortExtVar(PortID, 11)
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(YearNum, 3 + elindex)
                    Call VarElCalc()
                    PortCostRat = VarRat
                Else
                    PortCostRat = (PortExtVar(PortID, 11) / CostBase) ^ SeaEl(YearNum, 3 + elindex)
                End If
            Else
                If TripRates = "SubStrategy" Then
                    PortPopRat = ((PortExtVar(PortID, 9) * SeaTripRates(YearNum)) / PopBase) ^ SeaEl(YearNum, 1 + elindex)
                Else
                    PortPopRat = (PortExtVar(PortID, 9) / PopBase) ^ SeaEl(YearNum, 1 + elindex)
                End If
                PortGVARat = (PortExtVar(PortID, 10) / GVABase) ^ SeaEl(YearNum, 2 + elindex)
                PortCostRat = (PortExtVar(PortID, 11) / CostBase) ^ SeaEl(YearNum, 3 + elindex)
            End If
            FreightRat = PortPopRat * PortGVARat * PortCostRat
            If BaseFreight(FreightType) = 0 Then
                NewFreight(PortID, FreightType) = 0
            Else
                NewFreight(PortID, FreightType) = BaseFreight(FreightType) * FreightRat
            End If
            'check if capacity data exists
            portcap = PortExtVar(PortID, evindex)
            If portcap > -1 Then
                'if it does then check if new unconstrained traffic level exceeds the capacity constraint
                If NewFreight(PortID, FreightType) > portcap Then
                    'if it does then transfer the excess traffic to the latent freight variable
                    LatentFreight(FreightType) += NewFreight(PortID, FreightType) - portcap
                    'set the new traffic level to equal the constraint
                    NewFreight(PortID, FreightType) = portcap
                Else
                    'otherwise, need to check if there is any latent traffic to add in
                    NewFreight(PortID, FreightType) += LatentFreight(FreightType)
                    'set latent value to zero
                    LatentFreight(FreightType) = 0
                    'check if new freight traffic level now exceeds the capacity constraint
                    If NewFreight(PortID, FreightType) > portcap Then
                        'if it does then transfer the excess traffic to the latent freight variable
                        LatentFreight(FreightType) = NewFreight(PortID, FreightType) - portcap
                        'set the new traffic level to equal the constraint
                        NewFreight(PortID, FreightType) = portcap
                    End If
                End If
            End If
            FreightType += 1
        Loop
    End Sub

    Sub SeaFuelConsumption()
        Dim totalfreight As Double

        'get total freight tonnage, converting LoLo TEU to tons
        totalfreight = NewFreight(PortID, 1) + NewFreight(PortID, 2) + NewFreight(PortID, 3) + (NewFreight(PortID, 4) * 4.237035) + NewFreight(PortID, 5)
        'calculate gas oil and fuel oil consumption
        NewGasOil = totalfreight * 0.00286568 * PortExtVar(PortID, 12)
        NewFuelOil = totalfreight * 0.00352039 * PortExtVar(PortID, 12)
    End Sub

    Sub VarElCalc()
        Dim alpha, beta As Double
        Dim xnew As Double

        alpha = OldX / Math.Exp(OldEl)
        beta = (Math.Log(OldX / alpha)) / OldY
        xnew = alpha * Math.Exp(beta * NewY)
        VarRat = xnew / OldX

    End Sub

    Sub WritePortOutput()
        'write to output array
        OutputArray(InputCount, 0) = modelRunID
        OutputArray(InputCount, 1) = PortID
        OutputArray(InputCount, 2) = YearNum
        OutputArray(InputCount, 3) = NewFreight(PortID, 1)
        OutputArray(InputCount, 4) = NewFreight(PortID, 2)
        OutputArray(InputCount, 5) = NewFreight(PortID, 3)
        OutputArray(InputCount, 6) = NewFreight(PortID, 4)
        OutputArray(InputCount, 7) = NewFreight(PortID, 5)
        OutputArray(InputCount, 8) = NewGasOil
        OutputArray(InputCount, 9) = NewFuelOil

        'update the variables
        Dim evindex As Integer
        Dim cu As Double
        Dim newcapstring As String

        'loop through 5 freight types
        FreightType = 1
        Do While FreightType < 6
            evindex = FreightType + 3
            BaseFreight(FreightType) = NewFreight(PortID, FreightType)
            BaseCap(FreightType) = PortExtVar(PortID, evindex)
            If BuildInfra = True Then
                'check if capacity data exists
                If BaseCap(FreightType) > -1 Then
                    'check if capacity utilisation exceeds critical value
                    cu = BaseFreight(FreightType) / BaseCap(FreightType)
                    If cu >= CUCritValue Then
                        'if it does then add additional capacity
                        BaseCap(FreightType) += 1000
                        AddedCap(FreightType) += 1000
                        'write details to output file
                        Select Case FreightType
                            Case 1
                                newcapstring = PortID & "," & YearNum - 1 & ",1000,0,0,0,0"
                            Case 2
                                newcapstring = PortID & "," & YearNum - 1 & ",0,1000,0,0,0"
                            Case 3
                                newcapstring = PortID & "," & YearNum - 1 & ",0,0,1000,0,0"
                            Case 4
                                newcapstring = PortID & "," & YearNum - 1 & ",0,0,0,1000,0"
                            Case 5
                                newcapstring = PortID & "," & YearNum - 1 & ",0,0,0,0,1000"
                        End Select
                        'read newcapstring to array to writedata
                        Dim newcaps() As String
                        newcaps = Split(newcapstring, ",")
                        For i = 0 To 7
                            NewCapArray(NewCapNum, i) = newcaps(i)
                        Next
                        NewCapNum += 1
                    End If
                End If
            End If
            FreightType += 1
        Loop
        'PopBase = PortExtVar(PortID, 7)
        'GVABase = PortExtVar(PortID, 8)
        'CostBase = PortExtVar(PortID, 9)

        'write to temp array
        TempArray(InputCount, 0) = modelRunID
        TempArray(InputCount, 1) = YearNum
        TempArray(InputCount, 2) = PortID
        TempArray(InputCount, 3) = BaseFreight(1)
        TempArray(InputCount, 4) = BaseFreight(2)
        TempArray(InputCount, 5) = BaseFreight(3)
        TempArray(InputCount, 6) = BaseFreight(4)
        TempArray(InputCount, 7) = BaseFreight(5)
        For x = 1 To 5
            TempArray(InputCount, 7 + x) = AddedCap(x)
        Next

    End Sub

End Module
