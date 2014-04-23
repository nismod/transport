Module SeaExtVarCalc1pt4
    '1.2 creates an external variables file for the seaport freight model
    '1.2 fuel efficiency variable now added
    '1.3 this version allows input from the database
    'it also incorporates changes in fuel efficiency
    '1.3 mod fuel efficiency calculations corrected
    '1.4 fuel efficiency and cost calculation corrected

    Dim PortInputData As IO.FileStream
    Dim pi As IO.StreamReader
    Dim PortOutputData As IO.FileStream
    Dim po As IO.StreamWriter
    Dim PortCapData As IO.FileStream
    Dim pc As IO.StreamReader
    Dim PortNewCapData As IO.FileStream
    Dim pnc As IO.StreamWriter
    Dim pncr As IO.StreamReader
    Dim InputRow As String
    Dim OutputRow As String
    Dim PopGrowth As Double
    Dim GVAGrowth As Double
    Dim CostGrowth As Double
    Dim YearNum As Long
    Dim PortBaseData(47, 15) As String
    Dim PortCount As Long
    Dim PortNewData(47, 9) As String
    Dim CapID As Long
    Dim CapYear, CapNewYear As Integer
    Dim LBChange, DBChange, GCChange, LLChange, RRChange As Double
    Dim CapType, CapRow As String
    Dim CapChanged, Breakout As Boolean
    Dim ErrorString As String
    Dim stf As IO.StreamReader
    Dim stratstring, stratarray() As String
    Dim FuelEff(90) As Double
    Dim NewCapDetails(1, 6) As Double
    Dim CapCount As Long
    Dim tonnestobuild, captonnes As Double
    Dim sortarray(0) As String
    Dim sortedline As String
    Dim splitline() As String
    Dim arraynum As Long
    Dim AddingCap As Boolean

    Sub SeaEVMain()
        'get the input and output file names
        Call GetFiles()

        'read header row
        InputRow = pi.ReadLine

        'write header row to output file
        OutputRow = "PortID,Yeary,LBCapy,DBCapy,GCCapy,LLCapy,RRCapy,GORPopy,GORGvay,Costy,FuelEffy"
        'OutputRow = "Yeary,PortID,LBCapy,DBCapy,GCCapy,LLCapy,RRCapy,GORPopy,GORGvay,Costy,FuelEffy"
        po.WriteLine(OutputRow)

        'set scaling factors - as a default they are just set to be constant over time
        If SeaPopSource = "Constant" Then
            PopGrowth = 1.005
        End If
        If SeaEcoSource = "Constant" Then
            GVAGrowth = 1.016
        End If
        If SeaEneSource = "Constant" Then
            CostGrowth = 1.02
        End If

        PortCount = 1

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.3 change - now read this anyway to deal with compulsory enhancements
        'so we created another file containing sorted implemented capacity enhancements (in get files sub)
        'need initial file to be sorted by file type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array
        CapRow = pc.ReadLine
        CapCount = 0
        AddingCap = False
        tonnestobuild = 0
        Do Until CapRow Is Nothing
            Call GetCapData()
            Select Case CapType
                Case "C"
                    NewCapDetails(CapCount, 0) = CapID
                    NewCapDetails(CapCount, 1) = CapYear
                    NewCapDetails(CapCount, 2) = LBChange
                    NewCapDetails(CapCount, 3) = DBChange
                    NewCapDetails(CapCount, 4) = GCChange
                    NewCapDetails(CapCount, 5) = LLChange
                    NewCapDetails(CapCount, 6) = RRChange
                    CapNewYear = CapYear
                Case "O"
                    'then if adding optional capacity read all optional dated enhancements to intermediate array
                    If NewSeaCap = True Then
                        If CapYear >= 0 Then
                            NewCapDetails(CapCount, 0) = CapID
                            NewCapDetails(CapCount, 1) = CapYear
                            NewCapDetails(CapCount, 2) = LBChange
                            NewCapDetails(CapCount, 3) = DBChange
                            NewCapDetails(CapCount, 4) = GCChange
                            NewCapDetails(CapCount, 5) = LLChange
                            NewCapDetails(CapCount, 6) = RRChange
                            CapNewYear = CapYear
                        Else
                            'finally add all other enhancements to intermediate array until we have run out of additional capacity
                            captonnes = LBChange & DBChange & GCChange & LLChange & RRChange
                            If tonnestobuild >= captonnes Then
                                NewCapDetails(CapCount, 0) = CapID
                                NewCapDetails(CapCount, 1) = CapNewYear
                                NewCapDetails(CapCount, 2) = LBChange
                                NewCapDetails(CapCount, 3) = DBChange
                                NewCapDetails(CapCount, 4) = GCChange
                                NewCapDetails(CapCount, 5) = LLChange
                                NewCapDetails(CapCount, 6) = RRChange
                                tonnestobuild = tonnestobuild - captonnes
                            Else
                                Do Until tonnestobuild >= captonnes
                                    CapNewYear += 1
                                    If CapNewYear > 90 Then
                                        Breakout = True
                                        Exit Select
                                    End If
                                    tonnestobuild += NewSeaTonnes
                                Loop
                                NewCapDetails(CapCount, 0) = CapID
                                NewCapDetails(CapCount, 1) = CapNewYear
                                NewCapDetails(CapCount, 2) = LBChange
                                NewCapDetails(CapCount, 3) = DBChange
                                NewCapDetails(CapCount, 4) = GCChange
                                NewCapDetails(CapCount, 5) = LLChange
                                NewCapDetails(CapCount, 6) = RRChange
                                tonnestobuild = tonnestobuild - captonnes
                            End If
                        End If
                    Else
                        Exit Do
                    End If
            End Select
            If Breakout = True Then
                Exit Do
            End If
            CapRow = pc.ReadLine
            CapCount += 1
        Loop
        'then sort the intermediate array by port ID, then by year of implementation
        For v = 0 To 0
            sortarray(v) = NewCapDetails(v, 0) & "&" & NewCapDetails(v, 1) & "&" & v
        Next
        Array.Sort(sortarray)
        'write all lines to intermediate capacity file
        For v = 0 To 0
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            OutputRow = NewCapDetails(arraynum, 0) & "," & NewCapDetails(arraynum, 1) & "," & NewCapDetails(arraynum, 2) & "," & NewCapDetails(arraynum, 3) & "," & NewCapDetails(arraynum, 4) & "," & NewCapDetails(arraynum, 5) & "," & NewCapDetails(arraynum, 6)
            pnc.WriteLine(OutputRow)
        Next

        pc.Close()
        pnc.Close()

        'reopen the new capacity file as a reader
        PortNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "SeaFreightNewCap.csv", IO.FileMode.Open, IO.FileAccess.Read)
        pncr = New IO.StreamReader(PortNewCapData, System.Text.Encoding.Default)
        'read header
        pncr.ReadLine()
        'read first line of new capacity
        CapRow = pncr.ReadLine
        AddingCap = True
        Call GetCapData()

        'If NewSeaCap = True Then
        '    Call GetCapData()
        'End If

        'v1.3
        'get fuel efficiency values from the strategy file
        StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        stf = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
        'read header row
        stf.ReadLine()
        'v1.4 set FuelEff(0) to 1
        FuelEff(0) = 1
        For y = 1 To 90
            'read line from file
            stratstring = stf.ReadLine()
            stratarray = Split(stratstring, ",")
            FuelEff(y) = stratarray(69)
        Next
        stf.Close()

        'then loop through rest of rows in input data file
        Call CalcPortData()

        'Do Until PortCount > 47
        '    CapChanged = False
        '    Call CalcPortData()
        '    PortCount += 1
        'Loop

        pi.Close()
        po.Close()

    End Sub

    Sub GetFiles()
        Dim outstring As String

        PortInputData = New IO.FileStream(DirPath & "SeaFreightInputData.csv", IO.FileMode.Open, IO.FileAccess.Read)
        pi = New IO.StreamReader(PortInputData, System.Text.Encoding.Default)

        PortOutputData = New IO.FileStream(DirPath & EVFilePrefix & "SeaFreightExtVar.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        po = New IO.StreamWriter(PortOutputData, System.Text.Encoding.Default)

        'if capacity is changing then get capacity change file
        'v1.3 do this anyway to include compulsory changes
        PortCapData = New IO.FileStream(DirPath & CapFilePrefix & "SeaFreightCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        pc = New IO.StreamReader(PortCapData, System.Text.Encoding.Default)
        'read header row
        pc.ReadLine()
        'If NewSeaCap = True Then
        '    PortCapData = New IO.FileStream(DirPath & CapFilePrefix & "SeaFreightCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        '    pc = New IO.StreamReader(PortCapData, System.Text.Encoding.Default)
        '    'read header row
        '    pc.ReadLine()
        'End If
        'v1.3 new intermediate capacity file
        PortNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "SeaFreightNewCap.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        pnc = New IO.StreamWriter(PortNewCapData, System.Text.Encoding.Default)
        'write header row
        outstring = "PortID,ChangeYear,NewLBCap,NewDBCap,NewGCCap,NewLLCap,NewRRCap"
        pnc.WriteLine(outstring)

    End Sub

    Sub CalcPortData()
        Dim newcount As Integer
        Dim basecount As Integer
        'Dim GORID As Long
        Dim GORID(47, 1) As Long
        Dim keylookup As String
        Dim newval As Double
        Dim enestring As String
        Dim enearray As String()
        Dim DieselOld, DieselNew As Double
        Dim portdata() As String
        Dim i As Integer

        'Load in port base values 

        'InputRow = pi.ReadLine
        'PortBaseData = Split(InputRow, ",")
        'GORID = PortBaseData(14)

        'initializing sea energy source input file
        If SeaEneSource = "Database" Then
            'v1.3 altered so that scenario file is read directly as an input file
            ZoneEneFile = New IO.FileStream(DBaseEneFile, IO.FileMode.Open, IO.FileAccess.Read)
            zer = New IO.StreamReader(ZoneEneFile, System.Text.Encoding.Default)
            'read header row
            enestring = zer.ReadLine
            'read base year prices and split into variables
            enestring = zer.ReadLine
            enearray = Split(enestring, ",")
            DieselOld = enearray(2)
        End If

        YearNum = 1
        'calculate new values for port variables
        Do While YearNum < 91
            'calculate new values where needed
            'if including capacity changes, then check if there are any capacity changes for this zone
            'v1.3 changed to include compulsory capacity changes where construction has already begun
            'all this involves is removing the if newseacap = true clause, because this was already accounted for when generating the intermediate file, and adding a lineread above getcapdata because this sub was amended

            'read diesel new for the current year
            If SeaEneSource = "Database" Then
                enestring = zer.ReadLine
                enearray = Split(enestring, ",")
                DieselNew = enearray(2)
            End If

            'open input file for each year
            PortInputData = New IO.FileStream(DirPath & "SeaFreightInputData.csv", IO.FileMode.Open, IO.FileAccess.Read)
            pi = New IO.StreamReader(PortInputData, System.Text.Encoding.Default)
            InputRow = pi.ReadLine

            PortCount = 1
            Do Until PortCount > 47

                'read initial data if it is year 1
                CapChanged = False
                If YearNum = 1 Then
                    InputRow = pi.ReadLine
                    portdata = Split(InputRow, ",")
                    For i = 0 To 14
                        PortBaseData(PortCount, i) = portdata(i)
                    Next
                    GORID(PortCount, 1) = PortBaseData(PortCount, 14)
                End If


                If PortBaseData(PortCount, 0) = CapID Then
                    'if there are any capacity changes for this port, check if there are any capacity changes for this year

                    If YearNum = CapYear Then
                        'if there are, then update the capacity variables, and read in the next row from the capacity file
                        PortNewData(PortCount, 1) = PortBaseData(PortCount, 6) + LBChange
                        PortNewData(PortCount, 2) = PortBaseData(PortCount, 7) + DBChange
                        PortNewData(PortCount, 3) = PortBaseData(PortCount, 8) + GCChange
                        PortNewData(PortCount, 4) = PortBaseData(PortCount, 9) + LLChange
                        PortNewData(PortCount, 5) = PortBaseData(PortCount, 10) + RRChange
                        CapChanged = True
                        CapRow = pncr.ReadLine()
                        Call GetCapData()
                    ElseIf CapChanged = False Then
                        newcount = 1
                        basecount = 6
                        Do Until newcount = 6
                            PortNewData(PortCount, newcount) = PortBaseData(PortCount, basecount)
                            newcount += 1
                            basecount += 1
                        Loop
                    End If
                Else
                    newcount = 1
                    basecount = 6
                    Do Until newcount = 6
                        PortNewData(PortCount, newcount) = PortBaseData(PortCount, basecount)
                        newcount += 1
                        basecount += 1
                    Loop
                End If

                If SeaPopSource = "Constant" Then
                    PortNewData(PortCount, 6) = PortBaseData(PortCount, 11) * PopGrowth
                End If
                If SeaPopSource = "File" Then
                    'seaport model not yet set up for use with scaling files
                End If
                If SeaPopSource = "Database" Then
                    'if year is after 2093 then no population forecasts are available so assume population remains constant
                    'now modified as population data available up to 2100 - so should never need 'else'
                    If YearNum < 91 Then
                        keylookup = YearNum & "_" & GORID(PortCount, 1)
                        If PopYearLookup.TryGetValue(keylookup, newval) Then
                            PortNewData(PortCount, 6) = newval
                        Else
                            ErrorString = "population found in lookup table for zone " & GORID(PortCount, 1) & " in year " & YearNum
                            Call DictionaryMissingVal()
                        End If
                    Else
                        PortNewData(PortCount, 6) = PortBaseData(PortCount, 11)
                    End If
                End If

                If SeaEcoSource = "Constant" Then
                    PortNewData(PortCount, 7) = PortBaseData(PortCount, 12) * GVAGrowth
                ElseIf SeaEcoSource = "File" Then
                    'seaport model not yet set up for use with scaling files
                ElseIf SeaEcoSource = "Database" Then
                    'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                    'now modified as population data available up to 2100 - so should never need 'else'
                    If YearNum < 91 Then
                        keylookup = YearNum & "_" & GORID(PortCount, 1)
                        If EcoYearLookup.TryGetValue(keylookup, newval) Then
                            PortNewData(PortCount, 7) = newval
                        Else
                            ErrorString = "GVA found in lookup table for zone " & GORID(PortCount, 1) & " in year " & YearNum
                            Call DictionaryMissingVal()
                        End If
                    Else
                        PortNewData(PortCount, 7) = PortBaseData(PortCount, 12)
                    End If
                End If

                If SeaEneSource = "Constant" Then
                    PortNewData(PortCount, 8) = PortBaseData(PortCount, 13) * CostGrowth
                ElseIf SeaEneSource = "File" Then
                    'seaport model not yet set up for use with scaling files
                ElseIf SeaEneSource = "Database" Then
                    'v1.4 fuel efficiency change used instead of fuel efficiency
                    PortNewData(PortCount, 8) = PortBaseData(PortCount, 13) * (DieselNew / DieselOld) * (FuelEff(YearNum) / FuelEff(YearNum - 1))
                End If

                If SeaEneSource = "Database" Then
                    PortNewData(PortCount, 9) = FuelEff(YearNum)
                Else
                    PortNewData(PortCount, 9) = 1
                End If

                'write values to output file
                OutputRow = PortBaseData(PortCount, 0) & "," & YearNum
                'OutputRow = YearNum & "," & PortBaseData(0)
                newcount = 1
                Do Until newcount > 9
                    OutputRow = OutputRow & "," & PortNewData(PortCount, newcount)
                    newcount += 1
                Loop
                po.WriteLine(OutputRow)
                'set base values as previous new values
                newcount = 1
                basecount = 6
                Do Until newcount > 8
                    PortBaseData(PortCount, basecount) = PortNewData(PortCount, newcount)
                    newcount += 1
                    basecount += 1
                Loop

                PortCount += 1
            Loop

            If SeaEneSource = "Database" Then
                DieselOld = DieselNew
            End If

            'update year
            YearNum += 1
        Loop

        zer.Close()

    End Sub

    Sub GetCapData()

        Dim InputData() As String

        If CapRow Is Nothing Then
        Else
            InputData = Split(CapRow, ",")
            CapID = InputData(0)
            If InputData(1) = "" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = InputData(1) - 2010
                Else
                    CapYear = InputData(1)
                End If
            End If
            LBChange = InputData(2)
            DBChange = InputData(3)
            GCChange = InputData(4)
            LLChange = InputData(5)
            RRChange = InputData(6)
            If AddingCap = False Then
                CapType = InputData(7)
            End If
        End If

    End Sub

    Sub DictionaryMissingVal()
        LogLine = "No " & ErrorString & " when updating input files.  Model run terminated."
        lf.WriteLine(LogLine)
        lf.Close()
        MsgBox("Model run failed.  Please consult the log file for details.")
        End
    End Sub
End Module
