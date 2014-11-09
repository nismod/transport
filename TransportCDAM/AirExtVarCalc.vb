Module AirExtVarCalc
    'creates two external variable files for the air model, based on a single year's input data and growth factors for the other variables
    '1.2 this version allows capacity changes to be included
    '1.2 it also now includes fuel consumption variables
    '1.3 this version allows input from the database
    '1.4 fuel efficiency calculation corrected
    'now all file related functions are using databaseinterface
    '1.9 now the module can run with database connection and read/write from/to database

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
    Dim stratstring As String
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
    Dim enearray(91, 6) As String
    Dim stratarray(90, 95) As String
    Dim NodeInputArray(28, 16) As String
    Dim FlowInputArray(223, 11) As String
    Dim NodeOutputArray(29, 13) As String
    Dim FlowOutputArray(224, 7) As String
    Dim CapArray(47, 5) As String
    Dim NewCapArray(47, 5) As String
    Dim CapNum As Integer



    Public Sub AirEVMain()

        'get all related files
        Call GetFiles()

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
            'read data to energy array from the file
            Call ReadData("Energy", "", enearray, modelRunID)
            For y = 0 To 90
                FuelCost(y) = enearray(y + 1, 2)
            Next
        End If

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.3 change - now read this anyway to deal with compulsory enhancements
        'so we created another file containing sorted implemented capacity enhancements (in get files sub)
        'need initial file to be sorted by scheme type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array

        'initialise to read and write capacity change values
        CapCount = 0
        AddingCap = False
        TermToBuild = 0
        RunToBuild = 0
        CapNum = 1

        Do
            Call GetCapData()
            If CapArray(CapNum, 0) = 0 Then
                Exit Do
            End If
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
            'exit if year is greater than 2100
            If Breakout = True Then
                Exit Do
            End If
            CapNum += 1
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
            NewCapArray(v + 1, 0) = modelRunID
            For i = 0 To 3
                NewCapArray(v + 1, i + 1) = NewCapDetails(arraynum, i)
            Next
        Next

        Call WriteData("AirNode", "NewCap", NewCapArray)

        AddingCap = True
        'reset Capnum to read the first line
        CapNum = 1
        Call GetCapData()

        'If NewAirCap = True Then
        '    Call GetCapData()
        'End If

        YearNum = 1

        'then loop through rest of rows in input data file
        Do Until YearNum > 40
            Call CalcFlowData()

            'create file if it is the first year
            'it is now writting to database, therefore no difference if it is year 1 or not
            If YearNum = 1 Then
                Call WriteData("AirNode", "ExtVar", NodeOutputArray, , True)
                Call WriteData("AirFlow", "ExtVar", FlowOutputArray, , True)
            Else
                Call WriteData("AirNode", "ExtVar", NodeOutputArray, , False)
                Call WriteData("AirFlow", "ExtVar", FlowOutputArray, , False)
            End If


            YearNum += 1
        Loop


    End Sub

    Sub GetFiles()

        'read initial year files
        Call ReadData("AirNode", "Input", NodeInputArray, modelRunID, True)
        Call ReadData("AirFlow", "Input", FlowInputArray, modelRunID, True)

        'read capchange info
        Call ReadData("AirNode", "CapChange", CapArray, modelRunID)

        'now get strategy file too
        Call ReadData("SubStrategy", "", stratarray, modelRunID)

    End Sub

    Sub CalcFlowData()

        Dim NodeCount As Long
        Dim FlowCount As Long
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
                NodeOldData(NodeCount, 0) = NodeInputArray(NodeCount, 4)
                'pop
                NodeOldData(NodeCount, 1) = get_population_data_by_airportID(modelRunID, YearNum + 2010, NodeOldData(NodeCount, 0))
                'gva
                NodeOldData(NodeCount, 2) = get_gva_data_by_airportID(modelRunID, YearNum + 2010, NodeOldData(NodeCount, 0))
                'cost
                NodeOldData(NodeCount, 3) = NodeInputArray(NodeCount, 10) * 0.29
                airnodefixedcost(NodeCount) = NodeInputArray(NodeCount, 10) * 0.71
                'termcap
                NodeOldData(NodeCount, 4) = NodeInputArray(NodeCount, 8)
                'maxatm
                NodeOldData(NodeCount, 5) = NodeInputArray(NodeCount, 9)
                'psd
                NodeOldData(NodeCount, 6) = NodeInputArray(NodeCount, 11)
                'psi
                NodeOldData(NodeCount, 7) = NodeInputArray(NodeCount, 12)
                'lfd
                NodeOldData(NodeCount, 8) = NodeInputArray(NodeCount, 13)
                'lfi
                NodeOldData(NodeCount, 9) = NodeInputArray(NodeCount, 14)
                'inttripdist
                NodeOldData(NodeCount, 10) = NodeInputArray(NodeCount, 15)
                'fuelseatkm
                NodeOldData(NodeCount, 11) = 0.037251
                GORID(NodeCount) = NodeInputArray(NodeCount, 16)
                NodeCount += 1
            Loop

            FlowCount = 1
            Do While FlowCount < 224
                FlowOldData(FlowCount, 0) = FlowInputArray(FlowCount, 4)
                OZone(FlowCount) = FlowInputArray(FlowCount, 10)
                DZone(FlowCount) = FlowInputArray(FlowCount, 11)
                FlowOldData(FlowCount, 5) = FlowInputArray(FlowCount, 8) * 0.29
                airflowfixedcost(FlowCount) = FlowInputArray(FlowCount, 8) * 0.71
                FlowOldData(FlowCount, 1) = get_population_data_by_zoneID(modelRunID, YearNum + 2010, FlowOldData(FlowCount, 0), "OZ", "'air'", OZone(FlowCount))
                FlowOldData(FlowCount, 2) = get_population_data_by_zoneID(modelRunID, YearNum + 2010, FlowOldData(FlowCount, 0), "DZ", "'air'", DZone(FlowCount))
                FlowOldData(FlowCount, 3) = get_gva_data_by_zoneID(modelRunID, YearNum + 2010, FlowOldData(FlowCount, 0), "OZ", "'air'", OZone(FlowCount))
                FlowOldData(FlowCount, 4) = get_gva_data_by_zoneID(modelRunID, YearNum + 2010, FlowOldData(FlowCount, 0), "DZ", "'air'", DZone(FlowCount))

                'add in the Flow ID number as some are missing from the input file

                FlowLength(FlowCount) = FlowInputArray(FlowCount, 9)
                FlowCount += 1
            Loop
            'v1.4 set fueleffold value to 1
            FuelEffOld = 1
        Else
            'can use the new values from the previous year if no
        End If

        'get values from strategy file
        FuelEff = stratarray(YearNum, 68) / FuelEffOld

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
                'v1.9 now read pop data using database function
                If YearNum < 91 Then
                    NodeNewData(NodeCount, 1) = get_population_data_by_airportID(modelRunID, YearNum + 2010, NodeOldData(NodeCount, 0))
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
                'v1.9 now read gva data using database function
                'database does not have gva forecasts after year 2050, and the calculation is only available before year 2050
                If YearNum < 91 Then
                    NodeNewData(NodeCount, 2) = get_gva_data_by_airportID(modelRunID, YearNum + 2010, NodeOldData(NodeCount, 0))
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
                    CapNum += 1
                    Call GetCapData()
                End If
            End If
            NodeNewData(NodeCount, 4) = NodeOldData(NodeCount, 4) + AddTermCap
            NodeNewData(NodeCount, 5) = NodeOldData(NodeCount, 5) + AddATMCap
            NodeNewData(NodeCount, 6) = stratarray(YearNum, 83)
            NodeNewData(NodeCount, 7) = stratarray(YearNum, 84)
            NodeNewData(NodeCount, 8) = stratarray(YearNum, 85)
            NodeNewData(NodeCount, 9) = stratarray(YearNum, 86)
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
                    carbch = (NodeNewData(NodeCount, 10) * NodeNewData(NodeCount, 7) * 0.037251) * FuelEff * stratarray(YearNum, 73) * (stratarray(YearNum, 71) / 10)
                Else
                    carbch = 0
                End If
            Else
                carbch = 0
            End If

            'write node output row
            NodeOutputArray(NodeCount, 0) = modelRunID
            NodeOutputArray(NodeCount, 1) = NodeCount
            NodeOutputArray(NodeCount, 2) = YearNum
            NodeOutputArray(NodeCount, 3) = NodeNewData(NodeCount, 1)
            NodeOutputArray(NodeCount, 4) = NodeNewData(NodeCount, 2)
            newcost = airnodefixedcost(NodeCount) + NodeNewData(NodeCount, 3) + carbch
            NodeOutputArray(NodeCount, 5) = newcost

            For n = 4 To 11
                NodeOutputArray(NodeCount, n + 2) = NodeNewData(NodeCount, n)
            Next
            'OutVarCount = 1
            'Do Until OutVarCount > 11
            '    OutputRow = OutputRow & NodeNewData(NodeCount, OutVarCount) & ","
            '    OutVarCount += 1
            'Loop
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
                'v1.9 now read pop data using database function
                If YearNum < 91 Then
                    FlowNewData(FlowCount, 1) = get_population_data_by_zoneID(modelRunID, YearNum + 2010, FlowOldData(FlowCount, 0), "OZ", "'air'", OZone(FlowCount))
                    FlowNewData(FlowCount, 2) = get_population_data_by_zoneID(modelRunID, YearNum + 2010, FlowOldData(FlowCount, 0), "DZ", "'air'", DZone(FlowCount))
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
                'v1.9 now read gva data using database function
                'database does not have gva forecasts after year 2050, and the calculation is only available before year 2050
                If YearNum < 91 Then
                    FlowNewData(FlowCount, 3) = get_gva_data_by_zoneID(modelRunID, YearNum + 2010, FlowOldData(FlowCount, 0), "OZ", "'air'", OZone(FlowCount))
                    FlowNewData(FlowCount, 4) = get_gva_data_by_zoneID(modelRunID, YearNum + 2010, FlowOldData(FlowCount, 0), "DZ", "'air'", DZone(FlowCount))
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
                    carbch = (FlowLength(FlowCount) * stratarray(YearNum, 83) * 0.037251) * FuelEff * stratarray(YearNum, 73) * (stratarray(YearNum, 71) / 10)
                Else
                    carbch = 0
                End If
            Else
                carbch = 0
            End If

            'write flow output row
            FlowOutputArray(FlowCount, 0) = modelRunID
            FlowOutputArray(FlowCount, 1) = YearNum
            FlowOutputArray(FlowCount, 2) = FlowOldData(FlowCount, 0)
            FlowOutputArray(FlowCount, 3) = FlowNewData(FlowCount, 1)
            FlowOutputArray(FlowCount, 4) = FlowNewData(FlowCount, 2)
            FlowOutputArray(FlowCount, 5) = FlowNewData(FlowCount, 3)
            FlowOutputArray(FlowCount, 6) = FlowNewData(FlowCount, 4)
            newcost = airflowfixedcost(FlowCount) + FlowNewData(FlowCount, 5) + carbch
            FlowOutputArray(FlowCount, 7) = newcost
            'OutVarCount = 1
            'Do Until OutVarCount > 5
            '    OutputRow = OutputRow & FlowNewData(FlowCount, OutVarCount) & ","
            '    OutVarCount += 1
            'Loop

            'set old values as previous new values
            OutVarCount = 1
            Do Until OutVarCount > 5
                FlowOldData(FlowCount, OutVarCount) = FlowNewData(FlowCount, OutVarCount)
                OutVarCount += 1
            Loop
            FlowCount += 1
        Loop

        'v1.4 update FuelEffOld value
        FuelEffOld = stratarray(YearNum, 68)

    End Sub

    Sub GetCapData()
        'modified in v1.3
        'read cap data from CapArray until all rows are read
        If CapArray(CapNum, 0) <> "" Then
            CapID = CapArray(CapNum, 0)
            If CapArray(CapNum, 1) = "-1" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = CapArray(CapNum, 1) - 2010
                Else
                    CapYear = CapArray(CapNum, 1) - 2010
                End If
            End If
            TermCapChange = CapArray(CapNum, 2)
            ATMChange = CapArray(CapNum, 3)
            If AddingCap = False Then
                CapType = CapArray(CapNum, 4)
            End If
            CapNum += 1
        Else

        End If

    End Sub

    Sub DictionaryMissingVal()
        logarray(logNum, 0) = "No " & ErrorString & " when updating input files.  Model run terminated."
        logNum += 1
        Call WriteData("Logfile", "", logarray)
        MsgBox("Model run failed.  Please consult the log file for details.")
        End
    End Sub
End Module
