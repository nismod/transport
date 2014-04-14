Module Module1
    'this version completely revised to forecast five types of freight traffic - capable of dealing with capacity constraints and enhancements on a case by case basis if data available
    'it now also allows variable elasticities over time and by freight type
    'it now also includes a basic fuel consumption estimator
    'now also has the option to build additional infrastructure if capacity exceeds a certain level
    'now also has the option to use variable elasticities
    'now also includes changing fuel efficiency in fuel consumption calculations
    'now includes variable trip rate option

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
    Dim PortExtVar(10, 90) As Double
    Dim LatentFreight(5) As Double 'latent freight demand in the same order
    Dim PortPopRat As Double
    Dim PortGVARat As Double
    Dim PortCostRat As Double
    Dim FreightRat As Double
    Dim NewFreight(5) As Double
    Dim SeaEl(15, 90) As Double
    Dim NewGasOil, NewFuelOil As Double
    Dim AddedCap(5) As Long
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim SeaTripRates(90) As Double

    Public Sub SeaMain()
        'get input files and create output files
        Call SeaInputFiles()

        'get the elasticity values
        Call ReadSeaElasticities()

        'loop through all the flows in the input file
        Do
            InputRow = si.ReadLine
            'check if at end of file
            If InputRow Is Nothing Then
                'if we are then exit loop
                Exit Do
                'if not then run model on this port
            Else
                'update the input variables
                Call LoadPortInput()
                'get external variables for this port
                Call GetPortExtVar()
                'set year number to 1 (equivalent to 2011)
                YearNum = 1
                'set latent traffic levels to zero
                Call ResetPortLatent()
                'loop through all years calculating new port traffic and writing to output file
                Do While YearNum < 91
                    'calculate new traffic level, checking for capacity constraint
                    Call NewSeaFreightCalc()
                    'estimate fuel consumption
                    Call SeaFuelConsumption()
                    'write to output file
                    Call WritePortOutput()
                    'update base values
                    Call UpdatePortBase()
                    'move on to next year
                    YearNum += 1
                Loop
            End If
        Loop

        'close all files
        si.Close()
        se.Close()
        so.Close()
        If BuildInfra = True Then
            snc.Close()
        End If

    End Sub

    Sub SeaInputFiles()
        Dim row As String
        Dim stratarray() As String

        'get base input data
        SeaInputData = New IO.FileStream(DirPath & "SeaFreightInputData.csv", IO.FileMode.Open, IO.FileAccess.Read)
        si = New IO.StreamReader(SeaInputData, System.Text.Encoding.Default)
        'read header row
        row = si.ReadLine

        'get external variables file
        'check if updated version being used
        If UpdateExtVars = True Then
            If NewSeaCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If
        SeaExtVar = New IO.FileStream(DirPath & EVFilePrefix & "SeaFreightExtVar" & EVFileSuffix & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        se = New IO.StreamReader(SeaExtVar, System.Text.Encoding.Default)
        'read header row
        row = se.ReadLine

        'create output file
        SeaOutput = New IO.FileStream(DirPath & FilePrefix & "SeaFreightOutputData.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        so = New IO.StreamWriter(SeaOutput, System.Text.Encoding.Default)
        'write header row
        row = "PortID, Yeary, LiqBlky, DryBlky, GCargoy, LoLoy, RoRoy, GasOily, FuelOily"
        so.WriteLine(row)

        'get elasticities file
        SeaElasticities = New IO.FileStream(DirPath & "Elasticity Files\TR" & Strategy & "\SeaFreightElasticities.csv", IO.FileMode.Open, IO.FileAccess.Read)
        sel = New IO.StreamReader(SeaElasticities, System.Text.Encoding.Default)
        'read header row
        row = sel.ReadLine

        'if the model is building capacity then create new capacity file
        If BuildInfra = True Then
            SeaNewCap = New IO.FileStream(DirPath & FilePrefix & "SeaNewCap.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
            snc = New IO.StreamWriter(SeaNewCap, System.Text.Encoding.Default)
            'write header row
            row = "PortID,Yeary,LBCapAdded,DBCapAdded,GCCapAdded,LLCapAdded,RRCapAdded"
            snc.WriteLine(row)
        End If

        If TripRates = "Strategy" Then
            StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
            sst = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
            'read header row
            row = sst.ReadLine
            For r = 1 To 90
                row = sst.ReadLine
                stratarray = Split(row, ",")
                SeaTripRates(r) = stratarray(95)
            Next
            sst.Close()
        End If

    End Sub

    Sub LoadPortInput()
        PortDetails = Split(InputRow, ",")
        PortID = PortDetails(0)
        BaseFreight(1) = PortDetails(1)
        BaseFreight(2) = PortDetails(2)
        BaseFreight(3) = PortDetails(3)
        BaseFreight(4) = PortDetails(4)
        BaseFreight(5) = PortDetails(5)
        BaseCap(1) = PortDetails(6)
        BaseCap(2) = PortDetails(7)
        BaseCap(3) = PortDetails(8)
        BaseCap(4) = PortDetails(9)
        BaseCap(5) = PortDetails(10)
        PopBase = PortDetails(11)
        GVABase = PortDetails(12)
        CostBase = PortDetails(13)
        For x = 1 To 5
            AddedCap(x) = 0
        Next
    End Sub

    Sub GetPortExtVar()
        Dim yearcheck As Long
        Dim row As String
        Dim linedetails() As String
        Dim item As Integer

        yearcheck = 1
        Do While yearcheck < 91
            row = se.ReadLine()
            linedetails = Split(row, ",")
            item = 0
            Do Until item > 10
                PortExtVar(item, yearcheck) = linedetails(item)
                item += 1
            Loop
            yearcheck += 1
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
                PortExtVar(item, YearNum) += AddedCap(item - 1)
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
                    NewY = PortExtVar(7, YearNum) * SeaTripRates(YearNum)
                Else
                    NewY = PortExtVar(7, YearNum)
                End If
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(1 + elindex, YearNum)
                    Call VarElCalc()
                    PortPopRat = VarRat
                Else
                    If TripRates = "Strategy" Then
                        PortPopRat = ((PortExtVar(7, YearNum) * SeaTripRates(YearNum)) / PopBase) ^ SeaEl(1 + elindex, YearNum)
                    Else
                        PortPopRat = (PortExtVar(7, YearNum) / PopBase) ^ SeaEl(1 + elindex, YearNum)
                    End If
                End If
                'gva ratio
                OldY = GVABase
                NewY = PortExtVar(8, YearNum)
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(2 + elindex, YearNum)
                    Call VarElCalc()
                    PortGVARat = VarRat
                Else
                    PortGVARat = (PortExtVar(8, YearNum) / GVABase) ^ SeaEl(2 + elindex, YearNum)
                End If
                'cost ratio
                OldY = CostBase
                NewY = PortExtVar(9, YearNum)
                If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                    OldEl = SeaEl(3 + elindex, YearNum)
                    Call VarElCalc()
                    PortCostRat = VarRat
                Else
                    PortCostRat = (PortExtVar(9, YearNum) / CostBase) ^ SeaEl(3 + elindex, YearNum)
                End If
            Else
                If TripRates = "Strategy" Then
                    PortPopRat = ((PortExtVar(7, YearNum) * SeaTripRates(YearNum)) / PopBase) ^ SeaEl(1 + elindex, YearNum)
                Else
                    PortPopRat = (PortExtVar(7, YearNum) / PopBase) ^ SeaEl(1 + elindex, YearNum)
                End If
                PortGVARat = (PortExtVar(8, YearNum) / GVABase) ^ SeaEl(2 + elindex, YearNum)
                PortCostRat = (PortExtVar(9, YearNum) / CostBase) ^ SeaEl(3 + elindex, YearNum)
            End If
            FreightRat = PortPopRat * PortGVARat * PortCostRat
            NewFreight(FreightType) = BaseFreight(FreightType) * FreightRat
            'check if capacity data exists
            portcap = PortExtVar(evindex, YearNum)
            If portcap > -1 Then
                'if it does then check if new unconstrained traffic level exceeds the capacity constraint
                If NewFreight(FreightType) > portcap Then
                    'if it does then transfer the excess traffic to the latent freight variable
                    LatentFreight(FreightType) += NewFreight(FreightType) - portcap
                    'set the new traffic level to equal the constraint
                    NewFreight(FreightType) = portcap
                Else
                    'otherwise, need to check if there is any latent traffic to add in
                    NewFreight(FreightType) += LatentFreight(FreightType)
                    'set latent value to zero
                    LatentFreight(FreightType) = 0
                    'check if new freight traffic level now exceeds the capacity constraint
                    If NewFreight(FreightType) > portcap Then
                        'if it does then transfer the excess traffic to the latent freight variable
                        LatentFreight(FreightType) = NewFreight(FreightType) - portcap
                        'set the new traffic level to equal the constraint
                        NewFreight(FreightType) = portcap
                    End If
                End If
            End If
            FreightType += 1
        Loop
    End Sub

    Sub SeaFuelConsumption()
        Dim totalfreight As Double

        'get total freight tonnage, converting LoLo TEU to tons
        totalfreight = NewFreight(1) + NewFreight(2) + NewFreight(3) + (NewFreight(4) * 4.237035) + NewFreight(5)
        'calculate gas oil and fuel oil consumption
        NewGasOil = totalfreight * 0.00286568 * PortExtVar(10, YearNum)
        NewFuelOil = totalfreight * 0.00352039 * PortExtVar(10, YearNum)
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
        Dim row As String
        'concatenate data into row
        row = PortID & "," & YearNum & "," & NewFreight(1) & "," & NewFreight(2) & "," & NewFreight(3) & "," & NewFreight(4) & "," & NewFreight(5) & "," & NewGasOil & "," & NewFuelOil
        'write line to output file
        so.WriteLine(row)
    End Sub

    Sub UpdatePortBase()
        Dim evindex As Integer
        Dim cu As Double
        Dim newcapstring As String

        FreightType = 1
        Do While FreightType < 6
            evindex = FreightType + 1
            BaseFreight(FreightType) = NewFreight(FreightType)
            BaseCap(FreightType) = PortExtVar(evindex, YearNum)
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
                                newcapstring = PortID & "," & YearNum & ",1000,0,0,0,0"
                            Case 2
                                newcapstring = PortID & "," & YearNum & ",0,1000,0,0,0"
                            Case 3
                                newcapstring = PortID & "," & YearNum & ",0,0,1000,0,0"
                            Case 4
                                newcapstring = PortID & "," & YearNum & ",0,0,0,1000,0"
                            Case 5
                                newcapstring = PortID & "," & YearNum & ",0,0,0,0,1000"
                        End Select
                        snc.WriteLine(newcapstring)
                    End If
                End If
            End If
            FreightType += 1
        Loop
        PopBase = PortExtVar(7, YearNum)
        GVABase = PortExtVar(8, YearNum)
        CostBase = PortExtVar(9, YearNum)
    End Sub
End Module
