Module SeaExtVarCalc
    '1.2 creates an external variables file for the seaport freight model
    '1.2 fuel efficiency variable now added
    '1.3 this version allows input from the database
    'it also incorporates changes in fuel efficiency
    '1.3 mod fuel efficiency calculations corrected
    '1.4 fuel efficiency and cost calculation corrected
    'now all file related functions are using databaseinterface

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
    Dim stratstring As String
    Dim stratarray(90, 95) As String
    Dim FuelEff(90) As Double
    Dim NewCapDetails(1, 6) As Double
    Dim CapCount As Long
    Dim tonnestobuild, captonnes As Double
    Dim sortarray(0) As String
    Dim sortedline As String
    Dim splitline() As String
    Dim arraynum As Long
    Dim AddingCap As Boolean
    Dim CapNum As Integer
    Dim enearray(91, 6) As String
    Dim InputArray(47, 16) As String
    Dim CapArray(47, 8) As String
    Dim OutputArray(48, 11) As String


    Sub SeaEVMain()
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

        'read capchange info
        Call ReadData("Seaport", "CapChange", CapArray, modelRunID)

        'read energy file
        If SeaEneSource = "Database" Then
            Call ReadData("Energy", "", enearray, modelRunID)
        End If

        'read initial input data
        Call ReadData("Seaport", "Input", InputArray, modelRunID, True)

        'start read CapArray into the first row
        CapCount = 0
        AddingCap = False
        tonnestobuild = 0
        'read from the first row
        CapNum = 1
        Do
            Call GetCapData()
            'exit the loop if read to the end of the array
            If CapArray(CapNum, 0) = "" Then
                Exit Do
            End If
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
            'if the cap year over our range of 90 year, then exit
            If Breakout = True Then
                Exit Do
            End If
            CapCount += 1
        Loop
        'then sort the intermediate array by PortID, then by year of implementation
        For v = 0 To 0
            sortarray(v) = NewCapDetails(v, 0) & "&" & NewCapDetails(v, 1) & "&" & v
        Next
        Array.Sort(sortarray)

        For v = 0 To 0
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            For i = 0 To 6
                CapArray(v, i) = NewCapDetails(arraynum, i)
            Next
        Next
        'write all lines to intermediate capacity file
        Call WriteData("Seaport", "NewCap", CapArray)

        'read first line of new capacity
        AddingCap = True
        'reset CapNum to read the first row
        CapNum = 1
        Call GetCapData()

        'If NewSeaCap = True Then
        '    Call GetCapData()
        'End If

        'v1.3
        'get fuel efficiency values from the strategy file
        Call ReadData("SubStrategy", "", stratarray, modelRunID)
        'v1.4 set FuelEff(0) to 1
        FuelEff(0) = 1
        For y = 1 To 90
            FuelEff(y) = stratarray(y, 69)
        Next

        'then loop through rest of rows in input data file
        Call CalcPortData()



    End Sub
    Sub CalcPortData()
        Dim newcount As Integer
        Dim basecount As Integer
        Dim GORID(47, 1) As Long
        Dim keylookup As String
        Dim newval As Double
        Dim DieselOld, DieselNew As Double
        Dim i As Integer

        'use the data in the database file
        If SeaEneSource = "Database" Then
            DieselOld = enearray(1, 2)
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
                DieselNew = enearray(YearNum + 1, 2)
            End If

            PortCount = 1
            Do Until PortCount > 47

                'read initial data if it is year 1
                CapChanged = False
                If YearNum = 1 Then
                    For i = 0 To 10
                        PortBaseData(PortCount, i) = InputArray(PortCount, i + 4)
                    Next

                    'get GORPop and GORGva
                    PortBaseData(PortCount, 11) = get_population_data_by_economics_scenario_tr_zone(ScenarioID, modelRunYear, "sea", PortCount)
                    PortBaseData(PortCount, 12) = get_regional_gva_data_by_economics_scenario_tr_zone(ScenarioID, modelRunYear, "sea", PortCount)
                    PortBaseData(PortCount, 13) = InputArray(PortCount, 15)
                    GORID(PortCount, 1) = InputArray(PortCount, 16)
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
                'modelrun_id,year,port_id, LBCap,DBCap,GCCap,LLCap,RRCap,GORPop,GORGva,Cost,FuelEff
                'write values to output array
                OutputArray(PortCount, 0) = modelRunID
                OutputArray(PortCount, 1) = PortBaseData(PortCount, 0)
                OutputArray(PortCount, 2) = YearNum
                newcount = 1
                Do Until newcount > 9
                    OutputArray(PortCount, 2 + newcount) = PortNewData(PortCount, newcount)
                    newcount += 1
                Loop

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

            If YearNum = 1 Then
                Call WriteData("Seaport", "ExtVar", OutputArray, , True)
            Else
                Call WriteData("Seaport", "ExtVar", OutputArray, , False)
            End If

            'update year
            YearNum += 1
        Loop

        zer.Close()

    End Sub

    Sub GetCapData()

        'read CapArray until reach the end
        If CapArray(CapNum, 0) <> "" Then
            CapID = CapArray(CapNum, 0)
            If CapArray(CapNum, 1) = "" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = CapArray(CapNum, 1) - 2010
                Else
                    CapYear = CapArray(CapNum, 1) - 2010
                End If
            End If

            LBChange = CapArray(CapNum, 2)
            DBChange = CapArray(CapNum, 3)
            GCChange = CapArray(CapNum, 4)
            LLChange = CapArray(CapNum, 5)
            RRChange = CapArray(CapNum, 6)
            If AddingCap = False Then
                CapType = CapArray(CapNum, 7)
            End If
            CapNum += 1

        Else
            'if empty, do nothing
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
