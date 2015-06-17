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
    Dim FuelCost(1) As Double
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
    Dim NodeInputArray(28, 16) As String
    Dim FlowInputArray(223, 11) As String
    Dim NodeEVInputArray(28, 16) As String
    Dim FlowEVInputArray(223, 11) As String
    Dim NodeOutputArray(29, 14) As String
    Dim FlowOutputArray(224, 8) As String
    Dim CapArray(47, 6) As String
    Dim NewCapArray(47, 5) As String
    Dim CapNum As Integer
    Dim y As Object
    Dim yearIs2010 As Boolean = False




    Public Sub AirEVMain()

        'for year 2010, calculate as it is year 2011 and write output as year 2010
        If g_modelRunYear = 2010 Then
            'create data for year 2010
            g_modelRunYear += 1
            'Call Year2010()
            yearIs2010 = True
            'Exit Sub
        Else
            yearIs2010 = False
        End If

        'testmod
        If g_modelRunYear = 2012 Then
            g_modelRunYear = 2012
        End If

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
            'y = g_modelRunYear - g_initialYear + 1 'TODO - this needs fixing once we go to database for energy 
            FuelCost(0) = enearray(1, 2) 'last year
            FuelCost(1) = enearray(2, 2) 'this year
        End If

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.3 change - now read this anyway to deal with compulsory enhancements
        'so we created another file containing sorted implemented capacity enhancements (in get files sub)
        'need initial file to be sorted by scheme type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array

        'only do the cap change calculation for the intermediate cap change file if it is year 1
        If yearIs2010 = False And g_modelRunYear = g_initialYear Then
            'read capchange info
            Call DBaseInterface.ReadData("AirNode", "CapChange", CapArray, g_modelRunYear)

            'do capacity change requirement calculation
            Call CapChangeCalc()

            'write all lines to intermediate capacity file
            If Not NewCapArray Is Nothing Then
                Call DBaseInterface.WriteData("AirNode", "NewCap", NewCapArray)
            End If
        End If

        'read capchange info for the current year
        Call DBaseInterface.ReadData("AirNode", "NewCap", CapArray, g_modelRunYear)

        AddingCap = True
        'reset Capnum to read the first line
        CapNum = 1
        CapID = 0
        Call GetCapData()

        'If NewAirCap = True Then
        '    Call GetCapData()
        'End If

        Call CalcFlowData()

        'create file if it is the first year
        'it is now writting to database, therefore no difference if it is year 1 or not
        If g_modelRunYear = g_initialYear Then
            Call DBaseInterface.WriteData("AirNode", "ExtVar", NodeOutputArray, , True)
            Call DBaseInterface.WriteData("AirFlow", "ExtVar", FlowOutputArray, , True)
        Else
            Call DBaseInterface.WriteData("AirNode", "ExtVar", NodeOutputArray, , False)
            Call DBaseInterface.WriteData("AirFlow", "ExtVar", FlowOutputArray, , False)
        End If

        'minus a year if it is year 2010, for the next module
        If yearIs2010 = True Then g_modelRunYear -= 1



    End Sub

    Sub GetFiles()


        'read initial year files
        Call DBaseInterface.ReadData("AirNode", "Input", NodeInputArray, 2011)
        Call DBaseInterface.ReadData("AirFlow", "Input", FlowInputArray, 2011)
        If g_modelRunYear <> g_initialYear Then
            'read from previous year
            Call DBaseInterface.ReadData("AirNode", "ExtVar", NodeEVInputArray, g_modelRunYear - 1)
            Call DBaseInterface.ReadData("AirFlow", "ExtVar", FlowEVInputArray, g_modelRunYear - 1)
        End If

    End Sub

    Sub CalcFlowData()

        Dim NodeCount As Long
        Dim FlowCount As Long
        Dim OutVarCount As Integer
        Dim AddTermCap As Long
        Dim AddATMCap As Long
        Dim newcost As Double
        Dim carbch As Double

        'if this is the first year load in node base values and flow base values
        If g_modelRunYear = g_initialYear Then
            NodeCount = 1
            Do While NodeCount < 29
                NodeOldData(NodeCount, 0) = NodeInputArray(NodeCount, 1)
                'pop
                NodeOldData(NodeCount, 1) = get_population_data_by_airportID(g_modelRunYear - 1, NodeOldData(NodeCount, 0))
                'gva
                NodeOldData(NodeCount, 2) = get_gva_data_by_airportID(g_modelRunYear - 1, NodeOldData(NodeCount, 0))
                'cost
                NodeOldData(NodeCount, 3) = NodeInputArray(NodeCount, 7) * 0.29
                airnodefixedcost(NodeCount) = NodeInputArray(NodeCount, 7) * 0.71
                'termcap
                NodeOldData(NodeCount, 4) = NodeInputArray(NodeCount, 5)
                'maxatm
                NodeOldData(NodeCount, 5) = NodeInputArray(NodeCount, 6)
                'psd
                NodeOldData(NodeCount, 6) = NodeInputArray(NodeCount, 8)
                'psi
                NodeOldData(NodeCount, 7) = NodeInputArray(NodeCount, 9)
                'lfd
                NodeOldData(NodeCount, 8) = NodeInputArray(NodeCount, 10)
                'lfi
                NodeOldData(NodeCount, 9) = NodeInputArray(NodeCount, 11)
                'inttripdist
                NodeOldData(NodeCount, 10) = NodeInputArray(NodeCount, 12)
                'fuelseatkm
                NodeOldData(NodeCount, 11) = 0.037251
                GORID(NodeCount) = NodeInputArray(NodeCount, 2)
                NodeCount += 1
            Loop

            FlowCount = 1
            Do While FlowCount < 224
                FlowOldData(FlowCount, 0) = FlowInputArray(FlowCount, 1)
                OZone(FlowCount) = FlowInputArray(FlowCount, 7)
                DZone(FlowCount) = FlowInputArray(FlowCount, 8)
                FlowOldData(FlowCount, 5) = FlowInputArray(FlowCount, 5) * 0.29
                airflowfixedcost(FlowCount) = FlowInputArray(FlowCount, 5) * 0.71
                FlowOldData(FlowCount, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, OZone(FlowCount), "OZ", "'air'")
                FlowOldData(FlowCount, 2) = get_population_data_by_zoneID(g_modelRunYear - 1, DZone(FlowCount), "DZ", "'air'")
                FlowOldData(FlowCount, 3) = get_gva_data_by_zoneID(g_modelRunYear - 1, OZone(FlowCount), "OZ", "'air'")
                FlowOldData(FlowCount, 4) = get_gva_data_by_zoneID(g_modelRunYear - 1, DZone(FlowCount), "DZ", "'air'")

                'add in the Flow ID number as some are missing from the input file

                FlowLength(FlowCount) = FlowInputArray(FlowCount, 6)
                FlowCount += 1
            Loop
            'v1.4 set fueleffold value to 1
            FuelEffOld = 1
        Else
            'read from previous year's data

            'get airnode data from previous year
            NodeCount = 1
            Do While NodeCount < 29
                NodeOldData(NodeCount, 0) = NodeInputArray(NodeCount, 1)
                NodeOldData(NodeCount, 1) = get_population_data_by_airportID(g_modelRunYear - 1, NodeOldData(NodeCount, 0))
                NodeOldData(NodeCount, 2) = get_gva_data_by_airportID(g_modelRunYear - 1, NodeOldData(NodeCount, 0))
                NodeOldData(NodeCount, 3) = NodeEVInputArray(NodeCount, 15) 'TODO - this code makes no sense, there are only 13 fields in TR_IO_AirNodeExternalVariables?? -DONE extra field added

                OutVarCount = 4
                'v1.4 change, previously >10
                Do Until OutVarCount > 11
                    NodeOldData(NodeCount, OutVarCount) = NodeEVInputArray(NodeCount, OutVarCount + 3)
                    OutVarCount += 1
                Loop

                NodeCount += 1
            Loop

            'get airflow data from previous year
            FlowCount = 1
            Do While FlowCount < 224
                FlowOldData(FlowCount, 0) = FlowInputArray(FlowCount, 1)
                OZone(FlowCount) = FlowInputArray(FlowCount, 7)
                DZone(FlowCount) = FlowInputArray(FlowCount, 8)
                FlowOldData(FlowCount, 1) = get_population_data_by_zoneID(g_modelRunYear - 1, OZone(FlowCount), "OZ", "'air'")
                FlowOldData(FlowCount, 2) = get_population_data_by_zoneID(g_modelRunYear - 1, DZone(FlowCount), "DZ", "'air'")
                FlowOldData(FlowCount, 3) = get_gva_data_by_zoneID(g_modelRunYear - 1, OZone(FlowCount), "OZ", "'air'")
                FlowOldData(FlowCount, 4) = get_gva_data_by_zoneID(g_modelRunYear - 1, DZone(FlowCount), "DZ", "'air'")
                FlowOldData(FlowCount, 5) = FlowEVInputArray(FlowCount, 9)

                FlowCount += 1
            Loop


            FuelEffOld = stratarrayOLD(1, 70)

        End If

        'get values from strategy file
        FuelEff = stratarray(1, 70) / FuelEffOld

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
                NodeNewData(NodeCount, 1) = get_population_data_by_airportID(g_modelRunYear, NodeOldData(NodeCount, 0))
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
                NodeNewData(NodeCount, 2) = get_gva_data_by_airportID(g_modelRunYear, NodeOldData(NodeCount, 0))
            End If

            If AirEneSource = "Constant" Then
                NodeNewData(NodeCount, 3) = NodeOldData(NodeCount, 3) * CostGrowth
            ElseIf AirEneSource = "File" Then
                'air model not yet set up for use with scaling files
            ElseIf AirEneSource = "Database" Then
                NodeNewData(NodeCount, 3) = NodeOldData(NodeCount, 3) * (FuelCost(1) / FuelCost(0)) * FuelEff
            End If

            'if including capacity changes then check if there are any capacity changes in this year
            'v1.3 changed to include compulsory capacity changes where construction has already begun
            'all this involves is removing the if newrllcap = true clause, because this was already accounted for when generating the intermediate file, and adding a lineread above getcapdata because this sub was amended
            If g_modelRunYear = CapYear Then
                'if there are any capacity changes in this year, then check if there are any capacity changes at this airport
                If NodeCount = CapID Then
                    'if there are, then update the capacity variables, and read in the next row from the capacity file
                    AddTermCap = TermCapChange
                    AddATMCap = ATMChange
                    'write to CrossSector output for investment cost
                    'Airport terminals: £4,000 million each
                    'Airport runways: £8,000 million each
                    crossSectorArray(1, 3) += 4000 * TermCapChange / 20000000 + 8000 * ATMChange / 200000
                    Call GetCapData()
                End If
            End If
            NodeNewData(NodeCount, 4) = NodeOldData(NodeCount, 4) + AddTermCap
            NodeNewData(NodeCount, 5) = NodeOldData(NodeCount, 5) + AddATMCap
            NodeNewData(NodeCount, 6) = stratarray(1, 85)
            NodeNewData(NodeCount, 7) = stratarray(1, 86)
            NodeNewData(NodeCount, 8) = stratarray(1, 87)
            NodeNewData(NodeCount, 9) = stratarray(1, 88)
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
                If g_modelRunYear >= AirCaChYear Then
                    carbch = (NodeNewData(NodeCount, 10) * NodeNewData(NodeCount, 7) * 0.037251) * FuelEff * stratarray(1, 75) * (stratarray(1, 73) / 10)
                Else
                    carbch = 0
                End If
            Else
                carbch = 0
            End If

            If yearIs2010 = True Then g_modelRunYear -= 1
            'write node output row
            NodeOutputArray(NodeCount, 0) = g_modelRunID
            NodeOutputArray(NodeCount, 1) = NodeCount
            NodeOutputArray(NodeCount, 2) = g_modelRunYear
            NodeOutputArray(NodeCount, 3) = NodeNewData(NodeCount, 1)
            NodeOutputArray(NodeCount, 4) = NodeNewData(NodeCount, 2)
            newcost = airnodefixedcost(NodeCount) + NodeNewData(NodeCount, 3) + carbch
            NodeOutputArray(NodeCount, 5) = newcost

            For n = 4 To 11
                NodeOutputArray(NodeCount, n + 2) = NodeNewData(NodeCount, n)
            Next
            NodeOutputArray(NodeCount, 14) = NodeNewData(NodeCount, 3)

            'add back a year for next zone/link
            If yearIs2010 = True Then g_modelRunYear += 1

            'OutVarCount = 1
            'Do Until OutVarCount > 11
            '    OutputRow = OutputRow & NodeNewData(NodeCount, OutVarCount) & ","
            '    OutVarCount += 1
            'Loop
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
                FlowNewData(FlowCount, 1) = get_population_data_by_zoneID(g_modelRunYear, OZone(FlowCount), "OZ", "'air'")
                FlowNewData(FlowCount, 2) = get_population_data_by_zoneID(g_modelRunYear, DZone(FlowCount), "DZ", "'air'")
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
                FlowNewData(FlowCount, 3) = get_gva_data_by_zoneID(g_modelRunYear, OZone(FlowCount), "OZ", "'air'")
                FlowNewData(FlowCount, 4) = get_gva_data_by_zoneID(g_modelRunYear, DZone(FlowCount), "DZ", "'air'")
            End If

            If AirEneSource = "Constant" Then
                FlowNewData(FlowCount, 5) = FlowOldData(FlowCount, 5) * CostGrowth
            ElseIf AirEneSource = "File" Then
                'air model not yet set up for scaling files
            ElseIf AirEneSource = "Database" Then
                FlowNewData(FlowCount, 5) = FlowOldData(FlowCount, 5) * (FuelCost(1) / FuelCost(0)) * FuelEff
            End If

            'if applying a carbon charge then calculate it
            'calculation is: (base fuel units per journey * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
            'base fuel units per journey = averagedist * domplanesize * fuelperseatkm
            If AirCaCharge = True Then
                If g_modelRunYear >= AirCaChYear Then
                    carbch = (FlowLength(FlowCount) * stratarray(1, 85) * 0.037251) * FuelEff * stratarray(1, 75) * (stratarray(1, 73) / 10)
                Else
                    carbch = 0
                End If
            Else
                carbch = 0
            End If

            If yearIs2010 = True Then g_modelRunYear -= 1

            'write flow output row
            FlowOutputArray(FlowCount, 0) = g_modelRunID
            FlowOutputArray(FlowCount, 1) = g_modelRunYear
            FlowOutputArray(FlowCount, 2) = FlowOldData(FlowCount, 0)
            FlowOutputArray(FlowCount, 3) = FlowNewData(FlowCount, 1)
            FlowOutputArray(FlowCount, 4) = FlowNewData(FlowCount, 2)
            FlowOutputArray(FlowCount, 5) = FlowNewData(FlowCount, 3)
            FlowOutputArray(FlowCount, 6) = FlowNewData(FlowCount, 4)
            newcost = airflowfixedcost(FlowCount) + FlowNewData(FlowCount, 5) + carbch
            FlowOutputArray(FlowCount, 7) = newcost
            FlowOutputArray(FlowCount, 8) = FlowNewData(FlowCount, 5)
            'OutVarCount = 1
            'Do Until OutVarCount > 5
            '    OutputRow = OutputRow & FlowNewData(FlowCount, OutVarCount) & ","
            '    OutVarCount += 1
            'Loop

            If yearIs2010 = True Then g_modelRunYear += 1

            FlowCount += 1
        Loop


    End Sub

    Sub GetCapData()
        'modified in v1.3
        'read cap data from CapArray until all rows are read
        If CapArray Is Nothing Then Exit Sub 'TODO - this gets set off every time 'DONE it is meant to be, if there is no cap change data for the current year 
        If CapArray(CapNum, 1) <> "" Then
            CapID = CapArray(CapNum, 2)
            If CapArray(CapNum, 3) = "-1" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = CapArray(CapNum, 3)
                Else
                    CapYear = CapArray(CapNum, 3)
                End If
            End If
            TermCapChange = CapArray(CapNum, 4)
            ATMChange = CapArray(CapNum, 5)
            'If AddingCap = False Then
            '    CapType = CapArray(CapNum, 6)
            'End If
            CapNum += 1
        Else

        End If

    End Sub



    Sub CapChangeCalc()
        Dim CapGroupNum As Integer = 0
        Dim Cap As Integer = 0
        'initialise to read and write capacity change values
        CapCount = 0
        'addingcap is false when is reading from LU table
        AddingCap = False
        TermToBuild = 0
        RunToBuild = 0
        CapNewYear = 2010
        CapNum = 1

        If CapArray Is Nothing Then Exit Sub

        Do

            If CapArray Is Nothing Then Exit Sub 'TODO - this gets set off every time -DONE it is meant to be
            If CapArray(CapNum, 1) <> "" Then
                CapID = CapArray(CapNum, 1)
                If CapArray(CapNum, 2) = "-1" Then
                    CapYear = -1
                Else
                    If AddingCap = False Then
                        CapYear = CapArray(CapNum, 2)
                    Else
                        CapYear = CapArray(CapNum, 2)
                    End If
                End If
                TermCapChange = CapArray(CapNum, 3)
                ATMChange = CapArray(CapNum, 4)
                If AddingCap = False Then
                    CapType = CapArray(CapNum, 5)
                End If
                CapNum += 1
            Else

            End If

            If CapArray Is Nothing Then
                Exit Do
            End If
            If CapArray(CapNum, 1) = 0 Then
                Exit Do
            End If

            'if the capacity group combination include the compulsory and optional type projects
            If capChangeArray(1, 2) = True Then
            Select CapType
                    Case "C"
                        NewCapDetails(CapCount, 0) = CapID
                        NewCapDetails(CapCount, 1) = CapYear
                        NewCapDetails(CapCount, 2) = TermCapChange
                        NewCapDetails(CapCount, 3) = ATMChange
                        CapNewYear = CapYear
                        CapCount += 1
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
                                            If CapNewYear > 2100 Then
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
                                            If CapNewYear > 2100 Then
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
                            CapCount += 1
                        Else
                            'Exit Do
                        End If

                End Select
            End If

            'if the captype is the relevant capacity group, then read from the array
            CapGroupNum = 0
            Do Until CapGroupNum = capGroupArray.Length

                'if the capacity group array is empty (no additional capacity) then exit
                If capGroupArray(CapGroupNum) Is Nothing Then Exit Do

                If CapType = capGroupArray(CapGroupNum) Then
                    NewCapDetails(CapCount + Cap, 0) = CapID
                    NewCapDetails(CapCount + Cap, 1) = CapNewYear
                    NewCapDetails(CapCount + Cap, 2) = TermCapChange
                    NewCapDetails(CapCount + Cap, 3) = ATMChange

                    Cap += 1
                End If

                CapGroupNum += 1
            Loop

            'exit if year is greater than 2100
            If Breakout = True Then
                Exit Do
            End If


        Loop
        'then sort the intermediate array by year of implementation then by flow ID
        ReDim sortarray(CapCount - 1 + Cap)
        For v = 0 To (CapCount - 1 + Cap)
            padflow = String.Format("{0:000}", NewCapDetails(v, 0))
            padyear = String.Format("{0:00}", NewCapDetails(v, 1))
            sortarray(v) = padyear & "&" & padflow & "&" & v
        Next
        Array.Sort(sortarray)
        'write all lines to intermediate capacity file
        For v = 0 To (CapCount - 1 + Cap)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            NewCapArray(v + 1, 0) = g_modelRunID
            For i = 0 To 3
                NewCapArray(v + 1, i + 1) = NewCapDetails(arraynum, i)
            Next
            'NewCapArray(v + 1, 5) = "C"
        Next


    End Sub
End Module
