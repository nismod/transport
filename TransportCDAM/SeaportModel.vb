Module Module1
    'this version completely revised to forecast five types of freight traffic - capable of dealing with capacity constraints and enhancements on a case by case basis if data available
    'it now also allows variable elasticities over time and by freight type
    'it now also includes a basic fuel consumption estimator
    'now also has the option to build additional infrastructure if capacity exceeds a certain level
    'now also has the option to use variable elasticities
    'now also includes changing fuel efficiency in fuel consumption calculations
    'now includes variable trip rate option
    'v1.7 now corporate with Database function, read/write are using the function in database interface

    Dim SeaInputData As IO.FileStream
    Dim si As IO.StreamReader
    Dim SeaExtVar As IO.FileStream
    Dim se As IO.StreamReader
    Dim SeaOutput As IO.FileStream
    Dim so As IO.StreamWriter
    Dim SeaElasticities As IO.FileStream
    Dim sel As IO.StreamReader
    Dim SeaNewCap As IO.FileStream
    Dim snc As IO.StreamWriter
    Dim sst As IO.StreamReader
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
    Dim InputArray(47, 13) As String
    Dim OutputArray(47, 8) As String
    Dim TempArray(47, 18) As String
    Dim NewCapNum As Integer


    Public Sub SeaMain()

        'get input files and create output files
        Call SeaInputFiles()


        YearNum = 1
        Do While YearNum < 91

            'get external variables for this port this year
            Call ReadData("Seaport", "ExtVar", PortExtVar, , YearNum)

            'read from initial file if year 1, otherwise update from temp file
            If YearNum = 1 Then
                Call ReadData("Seaport", "Input", InputArray, True, YearNum)
            Else
                ReDim Preserve InputArray(47, 18)
                Call ReadData("Seaport", "Input", InputArray, False, YearNum)
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
            If YearNum = 1 Then
                Call WriteData("Seaport", "Output", OutputArray, TempArray, True)
                'if the model is building capacity then create new capacity file
                If BuildInfra = True Then
                    Call WriteData("Seaport", "SeaNewCap", NewCapArray, , True)
                End If
            Else
                Call WriteData("Seaport", "Output", OutputArray, TempArray, False)
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

        If TripRates = "Strategy" Then
            'get the strat values
            Call ReadData("Strategy", "", stratarray)
            For r = 1 To 90
                SeaTripRates(r) = stratarray(r, 95)
            Next
        End If

        'get the elasticity values
        Call ReadData("Seaport", "Elasticity", SeaEl)

    End Sub

    Sub LoadPortInput()

        'read initial input file if year 1, else use updated data
        If YearNum = 1 Then
            PortID = InputArray(InputCount, 0)
            BaseFreight(1) = InputArray(InputCount, 1)
            BaseFreight(2) = InputArray(InputCount, 2)
            BaseFreight(3) = InputArray(InputCount, 3)
            BaseFreight(4) = InputArray(InputCount, 4)
            BaseFreight(5) = InputArray(InputCount, 5)
            BaseCap(1) = InputArray(InputCount, 6)
            BaseCap(2) = InputArray(InputCount, 7)
            BaseCap(3) = InputArray(InputCount, 8)
            BaseCap(4) = InputArray(InputCount, 9)
            BaseCap(5) = InputArray(InputCount, 10)
            PopBase = InputArray(InputCount, 11)
            GVABase = InputArray(InputCount, 12)
            CostBase = InputArray(InputCount, 13)
            For x = 1 To 5
                AddedCap(x) = 0
            Next
        Else

            PortID = InputArray(InputCount, 0)
            BaseFreight(1) = InputArray(InputCount, 1)
            BaseFreight(2) = InputArray(InputCount, 2)
            BaseFreight(3) = InputArray(InputCount, 3)
            BaseFreight(4) = InputArray(InputCount, 4)
            BaseFreight(5) = InputArray(InputCount, 5)
            BaseCap(1) = InputArray(InputCount, 6)
            BaseCap(2) = InputArray(InputCount, 7)
            BaseCap(3) = InputArray(InputCount, 8)
            BaseCap(4) = InputArray(InputCount, 9)
            BaseCap(5) = InputArray(InputCount, 10)
            PopBase = InputArray(InputCount, 11)
            GVABase = InputArray(InputCount, 12)
            CostBase = InputArray(InputCount, 13)
            For x = 1 To 5
                AddedCap(x) = InputArray(InputCount, 13 + x)
            Next

        End If

    End Sub

    Sub GetPortExtVar()
        Dim port As Long
        Dim row As String
        Dim linedetails() As String
        Dim item As Integer

        port = 1
        Do While port < 48
            row = se.ReadLine()
            linedetails = Split(row, ",")
            item = 0
            Do Until item > 10
                PortExtVar(port, item) = linedetails(item)
                item += 1
            Loop
            port += 1
        Loop



    End Sub

    Sub ReadSeaElasticities()
        Dim row As String
        Dim elstring() As String
        Dim yearcheck As Integer
        Dim elcount As Integer

        yearcheck = 1

        Do
            'read in row from elasticities file
            row = sel.ReadLine
            If row Is Nothing Then
                Exit Do
            End If
            'split it into array - 1 is LBpop, 2 is LBgva, 3 is LBcost, 4-6 are dry bulk, 7-9 are general cargo, 10-12 are LoLo, 13-15 are RoRo
            elstring = Split(row, ",")
            elcount = 1
            Do While elcount < 16
                SeaEl(elcount, yearcheck) = elstring(elcount)
                elcount += 1
            Loop
            yearcheck += 1
        Loop

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

        If BuildInfra = True Then
            item = 2
            Do While item < 7
                PortExtVar(PortID, item) += AddedCap(item - 1)
                item += 1
            Loop
        End If

        'loop through all the freight types calculating new freight volumes
        FreightType = 1
        Do While FreightType < 6
            evindex = FreightType + 1
            elindex = (FreightType - 1) * 3
            'calculate ratios - now includes option to use variable elasticities
            If VariableEl = True Then
                OldX = BaseFreight(FreightType)
                'pop ratio
                OldY = PopBase
                If TripRates = "Strategy" Then
                    NewY = PortExtVar(PortID, 7) * SeaTripRates(YearNum)
                Else
                    NewY = PortExtVar(PortID, 7)
                End If
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(YearNum, 1 + elindex)
                    Call VarElCalc()
                    PortPopRat = VarRat
                Else
                    If TripRates = "Strategy" Then
                        PortPopRat = ((PortExtVar(PortID, 7) * SeaTripRates(YearNum)) / PopBase) ^ SeaEl(YearNum, 1 + elindex)
                    Else
                        PortPopRat = (PortExtVar(PortID, 7) / PopBase) ^ SeaEl(YearNum, 1 + elindex)
                    End If
                End If
                'gva ratio
                OldY = GVABase
                NewY = PortExtVar(PortID, 8)
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(YearNum, 2 + elindex)
                    Call VarElCalc()
                    PortGVARat = VarRat
                Else
                    PortGVARat = (PortExtVar(PortID, 8) / GVABase) ^ SeaEl(YearNum, 2 + elindex)
                End If
                'cost ratio
                OldY = CostBase
                NewY = PortExtVar(PortID, 9)
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(YearNum, 3 + elindex)
                    Call VarElCalc()
                    PortCostRat = VarRat
                Else
                    PortCostRat = (PortExtVar(PortID, 9) / CostBase) ^ SeaEl(YearNum, 3 + elindex)
                End If
            Else
                If TripRates = "Strategy" Then
                    PortPopRat = ((PortExtVar(PortID, 7) * SeaTripRates(YearNum)) / PopBase) ^ SeaEl(YearNum, 1 + elindex)
                Else
                    PortPopRat = (PortExtVar(PortID, 7) / PopBase) ^ SeaEl(YearNum, 1 + elindex)
                End If
                PortGVARat = (PortExtVar(PortID, 8) / GVABase) ^ SeaEl(YearNum, 2 + elindex)
                PortCostRat = (PortExtVar(PortID, 9) / CostBase) ^ SeaEl(YearNum, 3 + elindex)
            End If
            FreightRat = PortPopRat * PortGVARat * PortCostRat
            NewFreight(PortID, FreightType) = BaseFreight(FreightType) * FreightRat
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
        NewGasOil = totalfreight * 0.00286568 * PortExtVar(PortID, 10)
        NewFuelOil = totalfreight * 0.00352039 * PortExtVar(PortID, 10)
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
        OutputArray(InputCount, 0) = YearNum
        OutputArray(InputCount, 1) = PortID
        OutputArray(InputCount, 2) = NewFreight(PortID, 1)
        OutputArray(InputCount, 3) = NewFreight(PortID, 2)
        OutputArray(InputCount, 4) = NewFreight(PortID, 3)
        OutputArray(InputCount, 5) = NewFreight(PortID, 4)
        OutputArray(InputCount, 6) = NewFreight(PortID, 5)
        OutputArray(InputCount, 7) = NewGasOil
        OutputArray(InputCount, 8) = NewFuelOil

        'update the variables
        Dim evindex As Integer
        Dim cu As Double
        Dim newcapstring As String

        FreightType = 1
        Do While FreightType < 6
            evindex = FreightType + 1
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
                        Dim abc() As String
                        abc = Split(newcapstring, ",")
                        For i = 0 To 7
                            NewCapArray(NewCapNum, i) = abc(i)
                        Next
                        NewCapNum += 1
                    End If
                End If
            End If
            FreightType += 1
        Loop
        PopBase = PortExtVar(PortID, 7)
        GVABase = PortExtVar(PortID, 8)
        CostBase = PortExtVar(PortID, 9)

        'write to temp array
        TempArray(InputCount, 0) = PortID
        TempArray(InputCount, 1) = BaseFreight(1)
        TempArray(InputCount, 2) = BaseFreight(2)
        TempArray(InputCount, 3) = BaseFreight(3)
        TempArray(InputCount, 4) = BaseFreight(4)
        TempArray(InputCount, 5) = BaseFreight(5)
        TempArray(InputCount, 6) = BaseCap(1)
        TempArray(InputCount, 7) = BaseCap(2)
        TempArray(InputCount, 8) = BaseCap(3)
        TempArray(InputCount, 9) = BaseCap(4)
        TempArray(InputCount, 10) = BaseCap(5)
        TempArray(InputCount, 11) = PopBase
        TempArray(InputCount, 12) = GVABase
        TempArray(InputCount, 13) = CostBase
        For x = 1 To 5
            TempArray(InputCount, 13 + x) = AddedCap(x)
        Next

    End Sub

    'Sub UpdatePortBase()
    '    Dim evindex As Integer
    '    Dim cu As Double
    '    Dim newcapstring As String

    '    FreightType = 1
    '    Do While FreightType < 6
    '        evindex = FreightType + 1
    '        BaseFreight(FreightType) = NewFreight(PortID, FreightType)
    '        BaseCap(FreightType) = PortExtVar(PortID, evindex, YearNum)
    '        If BuildInfra = True Then
    '            'check if capacity data exists
    '            If BaseCap(FreightType) > -1 Then
    '                'check if capacity utilisation exceeds critical value
    '                cu = BaseFreight(FreightType) / BaseCap(FreightType)
    '                If cu >= CUCritValue Then
    '                    'if it does then add additional capacity
    '                    BaseCap(FreightType) += 1000
    '                    AddedCap(FreightType) += 1000
    '                    'write details to output file
    '                    Select Case FreightType
    '                        Case 1
    '                            newcapstring = PortID & "," & YearNum & ",1000,0,0,0,0"
    '                        Case 2
    '                            newcapstring = PortID & "," & YearNum & ",0,1000,0,0,0"
    '                        Case 3
    '                            newcapstring = PortID & "," & YearNum & ",0,0,1000,0,0"
    '                        Case 4
    '                            newcapstring = PortID & "," & YearNum & ",0,0,0,1000,0"
    '                        Case 5
    '                            newcapstring = PortID & "," & YearNum & ",0,0,0,0,1000"
    '                    End Select
    '                    snc.WriteLine(newcapstring)
    '                End If
    '            End If
    '        End If
    '        FreightType += 1
    '    Loop
    '    PopBase = PortExtVar(PortID, 7, YearNum)
    '    GVABase = PortExtVar(PortID, 8, YearNum)
    '    CostBase = PortExtVar(PortID, 9, YearNum)
    'End Sub

    Sub ReadSeaInput()

        InputCount = 1

        Do While InputCount < 48
            'read initial input file if year 1, else use updated data
            If YearNum = 1 Then
                InputRow = si.ReadLine
                PortDetails = Split(InputRow, ",")
                InputArray(InputCount, 0) = PortDetails(0)
                InputArray(InputCount, 1) = PortDetails(1)
                InputArray(InputCount, 2) = PortDetails(2)
                InputArray(InputCount, 3) = PortDetails(3)
                InputArray(InputCount, 4) = PortDetails(4)
                InputArray(InputCount, 5) = PortDetails(5)
                InputArray(InputCount, 6) = PortDetails(6)
                InputArray(InputCount, 7) = PortDetails(7)
                InputArray(InputCount, 8) = PortDetails(8)
                InputArray(InputCount, 9) = PortDetails(9)
                InputArray(InputCount, 10) = PortDetails(10)
                InputArray(InputCount, 11) = PortDetails(11)
                InputArray(InputCount, 12) = PortDetails(12)
                InputArray(InputCount, 13) = PortDetails(13)
            Else
                InputArray(InputCount, 0) = TempArray(InputCount, 0)
                InputArray(InputCount, 1) = TempArray(InputCount, 1)
                InputArray(InputCount, 2) = TempArray(InputCount, 2)
                InputArray(InputCount, 3) = TempArray(InputCount, 3)
                InputArray(InputCount, 4) = TempArray(InputCount, 4)
                InputArray(InputCount, 5) = TempArray(InputCount, 5)
                InputArray(InputCount, 6) = TempArray(InputCount, 6)
                InputArray(InputCount, 7) = TempArray(InputCount, 7)
                InputArray(InputCount, 8) = TempArray(InputCount, 8)
                InputArray(InputCount, 9) = TempArray(InputCount, 9)
                InputArray(InputCount, 10) = TempArray(InputCount, 10)
                InputArray(InputCount, 11) = TempArray(InputCount, 11)
                InputArray(InputCount, 12) = TempArray(InputCount, 12)
                InputArray(InputCount, 13) = TempArray(InputCount, 13)
            End If
            InputCount += 1
        Loop

    End Sub

    Sub WritePortTemp()
        Dim evindex As Integer
        Dim cu As Double
        Dim newcapstring As String

        'update the variables
        FreightType = 1
        Do While FreightType < 6
            evindex = FreightType + 1
            BaseFreight(FreightType) = NewFreight(PortID, FreightType)
            BaseCap(FreightType) = PortExtVar(evindex, PortID)
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
                        snc.WriteLine(newcapstring)
                    End If
                End If
            End If
            FreightType += 1
        Loop
        PopBase = PortExtVar(7, PortID)
        GVABase = PortExtVar(8, PortID)
        CostBase = PortExtVar(9, PortID)

        'write to temp array
        TempArray(InputCount, 0) = PortID
        TempArray(InputCount, 1) = BaseFreight(1)
        TempArray(InputCount, 2) = BaseFreight(2)
        TempArray(InputCount, 3) = BaseFreight(3)
        TempArray(InputCount, 4) = BaseFreight(4)
        TempArray(InputCount, 5) = BaseFreight(5)
        TempArray(InputCount, 6) = BaseCap(1)
        TempArray(InputCount, 7) = BaseCap(2)
        TempArray(InputCount, 8) = BaseCap(3)
        TempArray(InputCount, 9) = BaseCap(4)
        TempArray(InputCount, 10) = BaseCap(5)
        TempArray(InputCount, 11) = PopBase
        TempArray(InputCount, 12) = GVABase
        TempArray(InputCount, 13) = CostBase


    End Sub
End Module
