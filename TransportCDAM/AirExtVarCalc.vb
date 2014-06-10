﻿Module AirExtVarCalc1pt4
    'creates two external variable files for the air model, based on a single year's input data and growth factors for the other variables
    '1.2 this version allows capacity changes to be included
    '1.2 it also now includes fuel consumption variables
    '1.3 this version allows input from the database
    '1.4 fuel efficiency calculation corrected

    Dim AirNodeInputData As IO.FileStream
    Dim ni As IO.StreamReader
    Dim AirFlowInputData As IO.FileStream
    Dim fi As IO.StreamReader
    Dim NodeExtVarOutputData As IO.FileStream
    Dim nv As IO.StreamWriter
    Dim FlowExtVarOutputData As IO.FileStream
    Dim fv As IO.StreamWriter
    Dim AirNodeCapData As IO.FileStream
    Dim nc As IO.StreamReader
    Dim AirNewCapData As IO.FileStream
    Dim acn As IO.StreamWriter
    Dim acr As IO.StreamReader
    Dim NodeInputRow As String
    Dim FlowInputRow As String
    Dim OutputRow As String
    Dim NodeInputData() As String
    Dim FlowInputData() As String
    Dim PopGrowth As Double
    Dim GVAGrowth As Double
    Dim CostGrowth As Double
    Dim YearNum As Long
    Dim NodeOldData(28, 11) As Double
    Dim FlowOldData(223, 5) As Double
    Dim NodeNewData(28, 11) As Double
    Dim FlowNewData(223, 5) As Double
    Dim CapID As Long
    Dim CapYear, CapNewYear As Integer
    Dim ATMChange As Long
    Dim TermCapChange As Long
    Dim ErrorString As String
    Dim OZone(223), DZone(223) As Long
    Dim GORID(28) As Long
    Dim FuelCost(90) As Double
    Dim stf As IO.StreamReader
    Dim stratstring, stratarray() As String
    Dim FuelEff As Double
    Dim airnodefixedcost(28), airflowfixedcost(223) As Double
    Dim FlowLength(223) As Double
    Dim OutString As String
    Dim CapCount As Double
    Dim AddingCap As Boolean
    Dim CapType, CapRow As String
    Dim RunToBuild, TermToBuild As Double
    Dim NewCapDetails(836, 3) As Double
    Dim Breakout As Boolean
    Dim sortarray(836) As String
    Dim sortedline As String
    Dim splitline() As String
    Dim arraynum As Long
    Dim padflow, padyear As String
    Dim FuelEffOld As Double


    Public Sub AirEVMain()

        Dim enestring As String
        Dim enearray() As String

        'get the input and output file names
        Call GetFiles()

        'read header row for each file
        NodeInputRow = ni.ReadLine
        FlowInputRow = fi.ReadLine

        'write header row to output files
        OutputRow = "Yeary,AirportID,GORPopy,GORGvay,Costy,TermCapy,MaxATMy,PlaneSizeDomy,PlaneSizeInty,LFDomy,LFInty,IntTripDist,FuelSeatKm"
        nv.WriteLine(OutputRow)
        OutputRow = "Yeary,FlowID,PopOZy,PopDZy,GVAOZy,GVADZy,Costy"
        fv.WriteLine(OutputRow)

        'set scaling factors - as a default they are just set to be constant over time
        If AirPopSource = "Constant" Then
            PopGrowth = 1.005
        End If
        If AirEcoSource = "Constant" Then
            GVAGrowth = 1.016
        End If
        If AirEneSource = "Constant" Then
            CostGrowth = 1.02
        End If

        'if getting cost input from database then read in diesel cost data
        'v1.3 altered so that scenario file is read directly as an input file
        If AirEneSource = "Database" Then
            ZoneEneFile = New IO.FileStream(DBaseEneFile, IO.FileMode.Open, IO.FileAccess.Read)
            zer = New IO.StreamReader(ZoneEneFile, System.Text.Encoding.Default)
            'read header row
            enestring = zer.ReadLine
            For y = 0 To 90
                'read line of data
                enestring = zer.ReadLine
                enearray = Split(enestring, ",")
                FuelCost(y) = enearray(2)
            Next
            zer.Close()
        End If

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.3 change - now read this anyway to deal with compulsory enhancements
        'so we created another file containing sorted implemented capacity enhancements (in get files sub)
        'need initial file to be sorted by scheme type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array
        CapRow = nc.ReadLine
        CapCount = 0
        AddingCap = False
        TermToBuild = 0
        RunToBuild = 0
        Do Until CapRow Is Nothing
            Call GetCapData()
            Select Case CapType
                Case "C"
                    NewCapDetails(CapCount, 0) = CapID
                    NewCapDetails(CapCount, 1) = CapYear
                    NewCapDetails(CapCount, 2) = TermCapChange
                    NewCapDetails(CapCount, 3) = ATMChange
                    CapNewYear = CapYear
                Case "O"
                    'then if adding optional capacity read all optional dated enhancements to intermediate array
                    If NewAirCap = True Then
                        If CapYear >= 0 Then
                            NewCapDetails(CapCount, 0) = CapID
                            NewCapDetails(CapCount, 1) = CapYear
                            NewCapDetails(CapCount, 2) = TermCapChange
                            NewCapDetails(CapCount, 3) = ATMChange
                            CapNewYear = CapYear
                        Else
                            'finally add all other enhancements to intermediate array until we have run out of additional capacity
                            If TermCapChange > 0 Then
                                If TermToBuild >= TermCapChange Then
                                    NewCapDetails(CapCount, 0) = CapID
                                    NewCapDetails(CapCount, 1) = CapNewYear
                                    NewCapDetails(CapCount, 2) = TermCapChange
                                    NewCapDetails(CapCount, 3) = ATMChange
                                    TermToBuild = TermToBuild - TermCapChange
                                Else
                                    Do Until TermToBuild >= TermCapChange
                                        CapNewYear += 1
                                        If CapNewYear > 90 Then
                                            Breakout = True
                                            Exit Select
                                        End If
                                        TermToBuild += (NewAirTerm * 20000000)
                                        RunToBuild += (NewAirRun * 200000)
                                    Loop
                                    NewCapDetails(CapCount, 0) = CapID
                                    NewCapDetails(CapCount, 1) = CapNewYear
                                    NewCapDetails(CapCount, 2) = TermCapChange
                                    NewCapDetails(CapCount, 3) = ATMChange
                                    TermToBuild = TermToBuild - TermCapChange
                                End If
                            Else
                                If RunToBuild >= ATMChange Then
                                    NewCapDetails(CapCount, 0) = CapID
                                    NewCapDetails(CapCount, 1) = CapNewYear
                                    NewCapDetails(CapCount, 2) = TermCapChange
                                    NewCapDetails(CapCount, 3) = ATMChange
                                    RunToBuild = RunToBuild - ATMChange
                                Else
                                    Do Until RunToBuild >= ATMChange
                                        CapNewYear += 1
                                        If CapNewYear > 90 Then
                                            Breakout = True
                                            Exit Select
                                        End If
                                        TermToBuild += (NewAirTerm * 20000000)
                                        RunToBuild += (NewAirRun * 200000)
                                    Loop
                                    NewCapDetails(CapCount, 0) = CapID
                                    NewCapDetails(CapCount, 1) = CapNewYear
                                    NewCapDetails(CapCount, 2) = TermCapChange
                                    NewCapDetails(CapCount, 3) = ATMChange
                                    RunToBuild = RunToBuild - ATMChange
                                End If
                            End If
                        End If
                    Else
                        Exit Do
                    End If
            End Select
            If Breakout = True Then
                Exit Do
            End If
            CapRow = nc.ReadLine
            CapCount += 1
        Loop
        'then sort the intermediate array by year of implementation then by flow ID
        ReDim sortarray(CapCount - 1)
        For v = 0 To (CapCount - 1)
            padflow = String.Format("{0:000}", NewCapDetails(v, 0))
            padyear = String.Format("{0:00}", NewCapDetails(v, 1))
            sortarray(v) = padyear & "&" & padflow & "&" & v
        Next
        Array.Sort(sortarray)
        'write all lines to intermediate capacity file
        For v = 0 To (CapCount - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            OutputRow = NewCapDetails(arraynum, 0) & "," & NewCapDetails(arraynum, 1) & "," & NewCapDetails(arraynum, 2) & "," & NewCapDetails(arraynum, 3)
            acn.WriteLine(OutputRow)
        Next

        nc.Close()
        acn.Close()

        'reopen the capacity file as a reader
        AirNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "AirNodeNewCap.csv", IO.FileMode.Open, IO.FileAccess.Read)
        acr = New IO.StreamReader(AirNewCapData, System.Text.Encoding.Default)
        'read header
        acr.ReadLine()
        'read first line of new capacity
        CapRow = acr.ReadLine
        AddingCap = True
        Call GetCapData()

        'If NewAirCap = True Then
        '    Call GetCapData()
        'End If

        YearNum = 1

        'then loop through rest of rows in input data file
        Do Until YearNum > 90
            Call CalcFlowData()
            YearNum += 1
        Loop

        ni.Close()
        fi.Close()
        nv.Close()
        fv.Close()
        stf.Close()

    End Sub

    Sub GetFiles()

        AirNodeInputData = New IO.FileStream(DirPath & "AirNodeInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        ni = New IO.StreamReader(AirNodeInputData, System.Text.Encoding.Default)

        AirFlowInputData = New IO.FileStream(DirPath & "AirFlowInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        fi = New IO.StreamReader(AirFlowInputData, System.Text.Encoding.Default)

        NodeExtVarOutputData = New IO.FileStream(DirPath & EVFilePrefix & "AirNodeExtVar.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        nv = New IO.StreamWriter(NodeExtVarOutputData, System.Text.Encoding.Default)

        FlowExtVarOutputData = New IO.FileStream(DirPath & EVFilePrefix & "AirFlowExtVar.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        fv = New IO.StreamWriter(FlowExtVarOutputData, System.Text.Encoding.Default)

        'if capacity is changing then get capacity change file
        'v1.3 do this anyway to include compulsory changes
        AirNodeCapData = New IO.FileStream(DirPath & CapFilePrefix & "AirNodeCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        nc = New IO.StreamReader(AirNodeCapData, System.Text.Encoding.Default)
        'read header row
        nc.ReadLine()
        'v1.3 new intermediate capacity file
        AirNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "AirNodeNewCap.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        acn = New IO.StreamWriter(AirNewCapData, System.Text.Encoding.Default)
        'write header row
        OutString = "NodeID,ChangeYear,NewTermCap,NewATMCap"
        acn.WriteLine(OutString)

        'If NewAirCap = True Then
        '    AirNodeCapData = New IO.FileStream(DirPath & CapFilePrefix & "AirNodeCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        '    nc = New IO.StreamReader(AirNodeCapData, System.Text.Encoding.Default)
        '    'read header row
        '    nc.ReadLine()
        'End If

        'now get strategy file too
        StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        stf = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
        'read header row
        stf.ReadLine()

    End Sub

    Sub CalcFlowData()

        Dim NodeCount As Long
        Dim FlowCount As Long
        Dim VarCount As Integer
        Dim InVarCount As Integer
        Dim OutVarCount As Integer
        Dim AddTermCap As Long
        Dim AddATMCap As Long
        Dim keylookup As String
        Dim newval As Double
        Dim newcost As Double
        Dim carbch As Double

        'if this is the first year load in node base values and flow base values
        If YearNum = 1 Then
            NodeCount = 1
            Do While NodeCount < 29
                NodeInputRow = ni.ReadLine
                NodeInputData = Split(NodeInputRow, ",")
                'pop
                NodeOldData(NodeCount, 1) = NodeInputData(6)
                'gva
                NodeOldData(NodeCount, 2) = NodeInputData(7)
                'cost
                NodeOldData(NodeCount, 3) = NodeInputData(8) * 0.29
                airnodefixedcost(NodeCount) = NodeInputData(8) * 0.71
                'termcap
                NodeOldData(NodeCount, 4) = NodeInputData(4)
                'maxatm
                NodeOldData(NodeCount, 5) = NodeInputData(5)
                'psd
                NodeOldData(NodeCount, 6) = NodeInputData(9)
                'psi
                NodeOldData(NodeCount, 7) = NodeInputData(10)
                'lfd
                NodeOldData(NodeCount, 8) = NodeInputData(11)
                'lfi
                NodeOldData(NodeCount, 9) = NodeInputData(12)
                'inttripdist
                NodeOldData(NodeCount, 10) = NodeInputData(13)
                'fuelseatkm
                NodeOldData(NodeCount, 11) = 0.037251
                GORID(NodeCount) = NodeInputData(14)
                NodeCount += 1
            Loop

            FlowCount = 1
            Do While FlowCount < 224
                FlowInputRow = fi.ReadLine
                FlowInputData = Split(FlowInputRow, ",")
                VarCount = 1
                '***changed from 2 to 3
                InVarCount = 4
                Do Until VarCount > 5
                    FlowOldData(FlowCount, VarCount) = FlowInputData(InVarCount)
                    VarCount += 1
                    InVarCount += 1
                Loop
                FlowOldData(FlowCount, 5) = FlowOldData(FlowCount, 5) * 0.29
                airflowfixedcost(FlowCount) = FlowInputData(8) * 0.71
                OZone(FlowCount) = FlowInputData(10)
                DZone(FlowCount) = FlowInputData(11)
                'add in the Flow ID number as some are missing from the input file
                FlowOldData(FlowCount, 0) = FlowInputData(0)
                FlowLength(FlowCount) = FlowInputData(9)
                FlowCount += 1
            Loop
            'v1.4 set fueleffold value to 1
            FuelEffOld = 1
        Else
            'can use the new values from the previous year if no
        End If

        'get values from strategy file
        'read line from file
        stratstring = stf.ReadLine()
        stratarray = Split(stratstring, ",")
        FuelEff = stratarray(68) / FuelEffOld

        'calculate new node values first
        NodeCount = 1

        'loop through all nodes
        Do While NodeCount < 29
            'scale up all variable values for this year
            If AirPopSource = "Constant" Then
                NodeNewData(NodeCount, 1) = NodeOldData(NodeCount, 1) * PopGrowth
            End If
            If AirPopSource = "File" Then
                'air model not yet set up for use with scaling files
            End If
            If AirPopSource = "Database" Then
                'if year is after 2093 then no population forecasts are available so assume population remains constant
                'now modified as population data available up to 2100 - so should never need 'else'
                If YearNum < 91 Then
                    keylookup = YearNum & "_" & GORID(NodeCount)
                    If PopYearLookup.TryGetValue(keylookup, newval) Then
                        NodeNewData(NodeCount, 1) = newval
                    Else
                        ErrorString = "population found in lookup table for zone " & GORID(NodeCount) & " in year " & YearNum
                        Call DictionaryMissingVal()
                    End If
                Else
                    NodeNewData(NodeCount, 1) = NodeOldData(NodeCount, 1)
                End If
            End If
            If AirEcoSource = "Constant" Then
                NodeNewData(NodeCount, 2) = NodeOldData(NodeCount, 2) * GVAGrowth
            ElseIf AirEcoSource = "File" Then
                'air model not yet set up for use with scaling files
            ElseIf AirEcoSource = "Database" Then
                'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                'now modified as gva data available up to 2100 - so should never need 'else'
                If YearNum < 91 Then
                    keylookup = YearNum & "_" & GORID(NodeCount)
                    If EcoYearLookup.TryGetValue(keylookup, newval) Then
                        NodeNewData(NodeCount, 2) = newval
                    Else
                        ErrorString = "GVA found in lookup table for zone " & GORID(NodeCount) & " in year " & YearNum
                        Call DictionaryMissingVal()
                    End If
                Else
                    NodeNewData(NodeCount, 2) = NodeNewData(NodeCount, 2)
                End If
            End If

            If AirEneSource = "Constant" Then
                NodeNewData(NodeCount, 3) = NodeOldData(NodeCount, 3) * CostGrowth
            ElseIf AirEneSource = "File" Then
                'air model not yet set up for use with scaling files
            ElseIf AirEneSource = "Database" Then
                NodeNewData(NodeCount, 3) = NodeOldData(NodeCount, 3) * (FuelCost(YearNum) / FuelCost(YearNum - 1)) * FuelEff
            End If

            'if including capacity changes then check if there are any capacity changes in this year
            'v1.3 changed to include compulsory capacity changes where construction has already begun
            'all this involves is removing the if newrllcap = true clause, because this was already accounted for when generating the intermediate file, and adding a lineread above getcapdata because this sub was amended
            If YearNum = CapYear Then
                'if there are any capacity changes in this year, then check if there are any capacity changes at this airport
                If NodeCount = CapID Then
                    'if there are, then update the capacity variables, and read in the next row from the capacity file
                    AddTermCap = TermCapChange
                    AddATMCap = ATMChange
                    CapRow = acr.ReadLine()
                    Call GetCapData()
                End If
            End If
            NodeNewData(NodeCount, 4) = NodeOldData(NodeCount, 4) + AddTermCap
            NodeNewData(NodeCount, 5) = NodeOldData(NodeCount, 5) + AddATMCap
            NodeNewData(NodeCount, 6) = stratarray(83)
            NodeNewData(NodeCount, 7) = stratarray(84)
            NodeNewData(NodeCount, 8) = stratarray(85)
            NodeNewData(NodeCount, 9) = stratarray(86)
            NodeNewData(NodeCount, 10) = NodeOldData(NodeCount, 10)
            If AirEneSource = "Database" Then
                NodeNewData(NodeCount, 11) = NodeOldData(NodeCount, 11) * FuelEff
            Else
                NodeNewData(NodeCount, 11) = NodeOldData(NodeCount, 11)
            End If

            'if applying a carbon charge then calculate it
            'calculation is: (base fuel units per journey * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
            'base fuel units per journey = averagedist * intplanesize * fuelperseatkm
            If AirCaCharge = True Then
                If YearNum >= AirCaChYear Then
                    carbch = (NodeNewData(NodeCount, 10) * NodeNewData(NodeCount, 7) * 0.037251) * FuelEff * stratarray(73) * (stratarray(71) / 10)
                Else
                    carbch = 0
                End If
            Else
                carbch = 0
            End If

            'write node output row
            'v1.3 now calculates cost based on fuel and fixed costs
            OutputRow = YearNum & "," & NodeCount & ","
            For n = 1 To 2
                OutputRow = OutputRow & NodeNewData(NodeCount, n) & ","
            Next
            newcost = airnodefixedcost(NodeCount) + NodeNewData(NodeCount, 3) + carbch
            OutputRow = OutputRow & newcost & ","
            For n = 4 To 11
                OutputRow = OutputRow & NodeNewData(NodeCount, n) & ","
            Next
            'OutVarCount = 1
            'Do Until OutVarCount > 11
            '    OutputRow = OutputRow & NodeNewData(NodeCount, OutVarCount) & ","
            '    OutVarCount += 1
            'Loop
            nv.WriteLine(OutputRow)
            'set old values as previous new values
            OutVarCount = 1
            'v1.4 change, previously >10
            Do Until OutVarCount > 11
                NodeOldData(NodeCount, OutVarCount) = NodeNewData(NodeCount, OutVarCount)
                OutVarCount += 1
            Loop
            'reset additional capacity variables
            AddTermCap = 0
            AddATMCap = 0
            NodeCount += 1
        Loop

        'then calculate new flow values
        FlowCount = 1

        'loop through all flows
        Do While FlowCount < 224
            'scale up all variable values for this year
            If AirPopSource = "Constant" Then
                FlowNewData(FlowCount, 1) = FlowOldData(FlowCount, 1) * PopGrowth
                FlowNewData(FlowCount, 2) = FlowOldData(FlowCount, 2) * PopGrowth
            End If
            If AirPopSource = "File" Then
                'air model not yet set up for scaling files
            End If
            If AirPopSource = "Database" Then
                'if year is after 2093 then no population forecasts are available so assume population remains constant
                'now modified as population data available up to 2100 - so should never need 'else'
                If YearNum < 91 Then
                    keylookup = YearNum & "_" & OZone(FlowCount)
                    If PopYearLookup.TryGetValue(keylookup, newval) Then
                        FlowNewData(FlowCount, 1) = newval
                    Else
                        ErrorString = "population found in lookup table for zone " & OZone(FlowCount) & " in year " & YearNum & "with air flow " & FlowCount
                        Call DictionaryMissingVal()
                    End If
                    keylookup = YearNum & "_" & DZone(FlowCount)
                    If PopYearLookup.TryGetValue(keylookup, newval) Then
                        FlowNewData(FlowCount, 2) = newval
                    Else
                        ErrorString = "population found in lookup table for zone " & DZone(FlowCount) & " in year " & YearNum & "with air flow " & FlowCount
                        Call DictionaryMissingVal()
                    End If
                Else
                    FlowNewData(FlowCount, 1) = FlowOldData(FlowCount, 1)
                    FlowNewData(FlowCount, 2) = FlowOldData(FlowCount, 2)
                End If
            End If
            If AirEcoSource = "Constant" Then
                FlowNewData(FlowCount, 3) = FlowOldData(FlowCount, 3) * GVAGrowth
                FlowNewData(FlowCount, 4) = FlowOldData(FlowCount, 4) * GVAGrowth
            ElseIf AirEcoSource = "File" Then
                'air model not yet set up for scaling files
            ElseIf AirEcoSource = "Database" Then
                'now modified as gva data available up to 2100 - so should never need 'else'
                If YearNum < 91 Then
                    keylookup = YearNum & "_" & OZone(FlowCount)
                    If EcoYearLookup.TryGetValue(keylookup, newval) Then
                        FlowNewData(FlowCount, 3) = newval
                    Else
                        ErrorString = "gva found in lookup table for zone " & OZone(FlowCount) & " in year " & YearNum
                        Call DictionaryMissingVal()
                    End If
                    keylookup = YearNum & "_" & DZone(FlowCount)
                    If EcoYearLookup.TryGetValue(keylookup, newval) Then
                        FlowNewData(FlowCount, 4) = newval
                    Else
                        ErrorString = "gva found in lookup table for zone " & DZone(FlowCount) & " in year " & YearNum
                        Call DictionaryMissingVal()
                    End If
                Else
                    FlowNewData(FlowCount, 3) = FlowOldData(FlowCount, 3)
                    FlowNewData(FlowCount, 4) = FlowOldData(FlowCount, 4)
                End If
            End If

            If AirEneSource = "Constant" Then
                FlowNewData(FlowCount, 5) = FlowOldData(FlowCount, 5) * CostGrowth
            ElseIf AirEneSource = "File" Then
                'air model not yet set up for scaling files
            ElseIf AirEneSource = "Database" Then
                FlowNewData(FlowCount, 5) = FlowOldData(FlowCount, 5) * (FuelCost(YearNum) / FuelCost(YearNum - 1)) * FuelEff
            End If

            'if applying a carbon charge then calculate it
            'calculation is: (base fuel units per journey * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
            'base fuel units per journey = averagedist * domplanesize * fuelperseatkm
            If AirCaCharge = True Then
                If YearNum >= AirCaChYear Then
                    carbch = (FlowLength(FlowCount) * stratarray(83) * 0.037251) * FuelEff * stratarray(73) * (stratarray(71) / 10)
                Else
                    carbch = 0
                End If
            Else
                carbch = 0
            End If

            'write flow output row
            OutputRow = YearNum & "," & FlowOldData(FlowCount, 0) & ","
            For f = 1 To 4
                OutputRow = OutputRow & FlowNewData(FlowCount, f) & ","
            Next
            newcost = airflowfixedcost(FlowCount) + FlowNewData(FlowCount, 5) + carbch
            OutputRow = OutputRow & newcost & ","
            'OutVarCount = 1
            'Do Until OutVarCount > 5
            '    OutputRow = OutputRow & FlowNewData(FlowCount, OutVarCount) & ","
            '    OutVarCount += 1
            'Loop
            fv.WriteLine(OutputRow)
            'set old values as previous new values
            OutVarCount = 1
            Do Until OutVarCount > 5
                FlowOldData(FlowCount, OutVarCount) = FlowNewData(FlowCount, OutVarCount)
                OutVarCount += 1
            Loop
            FlowCount += 1
        Loop

        'v1.4 update FuelEffOld value
        FuelEffOld = stratarray(68)

    End Sub

    Sub GetCapData()
        'modified in v1.3
        If CapRow Is Nothing Then
        Else
            NodeInputData = Split(CapRow, ",")
            CapID = NodeInputData(0)
            If NodeInputData(1) = "-1" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = NodeInputData(1) - 2010
                Else
                    CapYear = NodeInputData(1)
                End If
            End If
            TermCapChange = NodeInputData(2)
            ATMChange = NodeInputData(3)
            If AddingCap = False Then
                CapType = NodeInputData(4)
            End If
        End If
    End Sub

    Sub DictionaryMissingVal()
        LogLine = "No " & ErrorString & " when updating input files.  Model run terminated."
        lf.WriteLine(LogLine)
        lf.Close()
        nv.Close()
        fv.Close()
        MsgBox("Model run failed.  Please consult the log file for details.")
        End
    End Sub
End Module