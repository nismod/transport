Imports System.IO

Module DBaseInterface
    'this gets the input files from the database, and converts them into a form suitable for input to the model files

    'this stores the total population values for each zone with the array index equal to the zoneID, 145 is Scotland and 146 is Wales
    Dim ZonePops(155) As Double
    Dim GorGVA(11) As Double
    Dim PopArray(), GORArray(), EcoArray() As String
    Dim Year As Long
    Dim YearCheck As Long
    Dim DictCreated As Boolean
    Dim DistZoneLookup As New Dictionary(Of String, Long)
    Dim zp, ze As StreamWriter
    Dim ZoneGVA As Double
    Dim ZoneID As Long
    Dim badregions(10) As String
    Dim badregioncount As Long
    Dim cmd As New Odbc.OdbcCommand
    Dim m_sConnString As String
    Dim m_conn As Odbc.OdbcConnection

    Sub ConnectToDBase()
        Try
            'If there is no connection to the database then establish one
            If m_conn Is Nothing Then
                'm_sConnString = "Driver={PostgreSQL ODBC Driver(ANSI)};DSN=PostgreSQL30;Server=localhost;Port=5432;Database=itrc_sos;UId=postgres;Password=76583Xucheng1;"
                m_sConnString = "Driver={PostgreSQL ODBC Driver(ANSI)};DSN=PostgreSQL30;Server=localhost;Port=5432;Database=itrc_sos;UId=postgres;Password=P0stgr3s;"
                m_conn = New Odbc.OdbcConnection(m_sConnString)
                m_conn.Open()
            End If

        Catch ex As Exception
            MsgBox(ex.Message)
            Throw (ex)
        End Try

    End Sub

    Sub DBaseInputMain()

        'set up the overall database check variable
        If DBasePop = True Then
            DBaseCheck = True
        ElseIf DBaseEco = True Then
            DBaseCheck = True
        ElseIf DBaseEne = True Then
            DBaseCheck = True
        Else
            DBaseCheck = False
        End If
        'check if we are getting population data from the database
        If DBasePop = True Then
            Call GetDBasePop()
            RdLPopSource = "Database"
            RdZPopSource = "Database"
            RlLPopSource = "Database"
            RlZPopSource = "Database"
            AirPopSource = "Database"
            SeaPopSource = "Database"
        End If
        'check if we are getting economic data from the database
        If DBaseEco = True Then
            Call GetDBaseEco()
            RdLEcoSource = "Database"
            RdZEcoSource = "Database"
            RlLEcoSource = "Database"
            RlZEcoSource = "Database"
            AirPopSource = "Database"
            SeaPopSource = "Database"
        End If
        'check if we are getting energy cost data from the database
        If DBaseEne = True Then
            Call GetDBaseEne()
            RdLEneSource = "Database"
            RdZEneSource = "Database"
            RlLEneSource = "Database"
            RlZEneSource = "Database"
            AirEneSource = "Database"
            SeaEneSource = "Database"
        End If
    End Sub

    Sub GetDBasePop()
        Dim ScenarioPopFile As FileStream
        Dim dp As StreamReader
        Dim GORPopFile As FileStream
        Dim gp As StreamReader
        Dim PopLine As String
        Dim ZonePopRow As String
        Dim zonecount As Long
        Dim GORLine As String
        Dim GORYear As Long
        Dim rowtype As String

        badregioncount = 0

        'set up input file
        ScenarioPopFile = New FileStream(DBasePopFile, FileMode.Open, FileAccess.Read)
        dp = New StreamReader(ScenarioPopFile, System.Text.Encoding.Default)
        'also GOR pop file as these are needed for air node model
        If DBasePopG = True Then
            GORPopFile = New FileStream(DBasePopGFile, FileMode.Open, FileAccess.Read)
            gp = New StreamReader(GORPopFile, System.Text.Encoding.Default)
        End If

        'set up output file
        ZonePopFile = New FileStream(DirPath & "ZoneScenarioPopFile.csv", FileMode.Create, FileAccess.Write)
        zp = New StreamWriter(ZonePopFile, System.Text.Encoding.Default)
        'write header row
        ZonePopRow = "Year,ITRCZone,Pop"
        zp.WriteLine(ZonePopRow)

        'set up district lookup table based on lookup file
        Call CreateDistLookup()

        'read header row
        PopLine = dp.ReadLine
        If DBasePopG = True Then
            'read header
            GORLine = gp.ReadLine
            'read first data line and split it
            GORLine = gp.ReadLine
            GORArray = Split(GORLine, ",")
            GORYear = GORArray(0)
        End If
        'write file name to log file
        logarray(logNum, 0) = "Population data taken from file " & DBasePopFile
        logNum += 1
        'read input line, split it and get year
        PopLine = dp.ReadLine
        PopArray = Split(PopLine, ",")
        Year = PopArray(0)
        Do Until PopLine Is Nothing
            'set year check
            YearCheck = Year
            Call DistrictLookup()
            Do
                'get input line
                PopLine = dp.ReadLine
                If PopLine Is Nothing Then
                    Exit Do
                End If
                PopArray = Split(PopLine, ",")
                'check year against year check
                Year = PopArray(0)
                If YearCheck = Year Then
                    'need to check if this is a births/deaths/migration row and ignore if so
                    rowtype = PopArray(2)
                    Select Case rowtype
                        Case "Births", "Deaths", "Migration"
                            'do nothing
                        Case Else
                            '180714 modification
                            If PopArray(1) = "Persons" Then
                                'do nothing
                            Else
                                'call district lookup
                                Call DistrictLookup()
                            End If

                    End Select
                Else
                    Exit Do
                End If
            Loop
            If SWDisagg = False Then
                'allocate Scottish and Welsh pops to counties
                ZonePops(1) = ZonePops(145) * 0.041573
                ZonePops(2) = ZonePops(145) * 0.047068
                ZonePops(3) = ZonePops(145) * 0.021179
                ZonePops(4) = ZonePops(145) * 0.017081
                ZonePops(24) = ZonePops(145) * 0.009689
                ZonePops(34) = ZonePops(145) * 0.028379
                ZonePops(35) = ZonePops(145) * 0.027632
                ZonePops(37) = ZonePops(145) * 0.023017
                ZonePops(38) = ZonePops(145) * 0.02003
                ZonePops(39) = ZonePops(145) * 0.01867
                ZonePops(40) = ZonePops(145) * 0.017138
                ZonePops(43) = ZonePops(145) * 0.093083
                ZonePops(44) = ZonePops(145) * 0.005017
                ZonePops(46) = ZonePops(145) * 0.029355
                ZonePops(47) = ZonePops(145) * 0.069894
                ZonePops(49) = ZonePops(145) * 0.113515
                ZonePops(59) = ZonePops(145) * 0.042434
                ZonePops(60) = ZonePops(145) * 0.015281
                ZonePops(75) = ZonePops(145) * 0.01553
                ZonePops(78) = ZonePops(145) * 0.016794
                ZonePops(82) = ZonePops(145) * 0.025889
                ZonePops(84) = ZonePops(145) * 0.062502
                ZonePops(92) = ZonePops(145) * 0.003849
                ZonePops(95) = ZonePops(145) * 0.028302
                ZonePops(103) = ZonePops(145) * 0.032611
                ZonePops(106) = ZonePops(145) * 0.021619
                ZonePops(107) = ZonePops(145) * 0.004289
                ZonePops(111) = ZonePops(145) * 0.021332
                ZonePops(113) = ZonePops(145) * 0.059726
                ZonePops(118) = ZonePops(145) * 0.017215
                ZonePops(134) = ZonePops(145) * 0.017349
                ZonePops(135) = ZonePops(145) * 0.032955
                ZonePops(9) = ZonePops(146) * 0.022751
                ZonePops(12) = ZonePops(146) * 0.04477
                ZonePops(16) = ZonePops(146) * 0.057575
                ZonePops(18) = ZonePops(146) * 0.113454
                ZonePops(19) = ZonePops(146) * 0.060103
                ZonePops(21) = ZonePops(146) * 0.025578
                ZonePops(25) = ZonePops(146) * 0.036887
                ZonePops(29) = ZonePops(146) * 0.032164
                ZonePops(48) = ZonePops(146) * 0.049792
                ZonePops(53) = ZonePops(146) * 0.039581
                ZonePops(61) = ZonePops(146) * 0.022817
                ZonePops(73) = ZonePops(146) * 0.018527
                ZonePops(77) = ZonePops(146) * 0.029303
                ZonePops(79) = ZonePops(146) * 0.045701
                ZonePops(80) = ZonePops(146) * 0.046998
                ZonePops(94) = ZonePops(146) * 0.038949
                ZonePops(100) = ZonePops(146) * 0.043672
                ZonePops(104) = ZonePops(146) * 0.077931
                ZonePops(123) = ZonePops(146) * 0.077332
                ZonePops(126) = ZonePops(146) * 0.041577
                ZonePops(129) = ZonePops(146) * 0.030101
                ZonePops(143) = ZonePops(146) * 0.044437
            End If

            'if necessary do the other GOR pops from the GOR pop file
            If DBasePopG = True Then
                Do
                    If GORYear = YearCheck Then
                        'need to check if this is a births/deaths/migration row and ignore if so
                        rowtype = GORArray(2)
                        Select Case rowtype
                            Case "Births", "Deaths", "Migration"
                                'do nothing
                            Case Else
                                Call GORLookup()
                        End Select
                        GORLine = gp.ReadLine
                        If GORLine Is Nothing Then
                            Exit Do
                        Else
                            GORArray = Split(GORLine, ",")
                            GORYear = GORArray(0)
                        End If
                    Else
                        Exit Do
                    End If
                Loop
            End If

            'write population output
            zonecount = 1
            If SWDisagg = True Then
                Do While zonecount < 145
                    ZonePopRow = YearCheck & "," & zonecount & "," & ZonePops(zonecount)
                    zp.WriteLine(ZonePopRow)
                    zonecount += 1
                Loop
            Else
                Do While zonecount < 147
                    ZonePopRow = YearCheck & "," & zonecount & "," & ZonePops(zonecount)
                    zp.WriteLine(ZonePopRow)
                    zonecount += 1
                Loop
            End If
            'if necessary also write the GOR pops
            If DBasePopG = True Then
                Do While zonecount < 156
                    ZonePopRow = YearCheck & "," & zonecount & "," & ZonePops(zonecount)
                    zp.WriteLine(ZonePopRow)
                    zonecount += 1
                Loop
            End If
            'redimension pop array
            ReDim ZonePops(155)
            If PopLine Is Nothing Then
                Exit Do
            End If
        Loop

        dp.Close()
        zp.Close()
        If DBasePopG = True Then
            gp.Close()
        End If

    End Sub

    Sub GetDBaseEco()
        Dim ScenarioEcoFile As FileStream
        Dim de As StreamReader
        Dim ZoneEcoRow As String
        Dim EcoLine As String

        badregioncount = 0

        'set up input file - file name provided by user in form
        ScenarioEcoFile = New FileStream(DBaseEcoFile, FileMode.Open, FileAccess.Read)
        de = New StreamReader(ScenarioEcoFile, System.Text.Encoding.Default)

        'set up output file
        ZoneEcoFile = New FileStream(DirPath & "ZoneScenarioEcoFile.csv", FileMode.Create, FileAccess.Write)
        ze = New StreamWriter(ZoneEcoFile, System.Text.Encoding.Default)

        'write header row
        ZoneEcoRow = "Year,ITRCZone,GVA"
        ze.WriteLine(ZoneEcoRow)

        'read header row
        EcoLine = de.ReadLine
        'write file name to log file
        logarray(logNum, 0) = "Economy data taken from file " & DBaseEcoFile
        logNum += 1
        'read input line, split it and get year
        EcoLine = de.ReadLine
        EcoArray = Split(EcoLine, ",")
        Year = EcoArray(0)
        Do Until EcoLine Is Nothing
            'set year check
            YearCheck = Year
            'call economy region lookup
            Call EcoRegionLookup()
            Do
                'get input line
                EcoLine = de.ReadLine
                If EcoLine Is Nothing Then
                    Exit Do
                End If
                EcoArray = Split(EcoLine, ",")
                'check year against year check
                Year = EcoArray(0)
                If YearCheck = Year Then
                    'call district lookup
                    Call EcoRegionLookup()
                Else
                    Exit Do
                End If
            Loop
            'loop through all zones assigning GOR GVA and writing output
            ZoneID = 1
            Do While ZoneID < 156
                'assign GOR GVA to zones - note that this includes GOR totals for the air and sea models
                Call AssignZoneGVA()
                'write gva output
                ZoneEcoRow = YearCheck & "," & ZoneID & "," & ZoneGVA
                ze.WriteLine(ZoneEcoRow)
                ZoneID += 1
            Loop
            'redimension gva array
            ReDim GorGVA(11)
            If EcoLine Is Nothing Then
                Exit Do
            End If
        Loop

        de.Close()
        ze.Close()

    End Sub

    Sub GetDBaseEne()
        'TODO ***NEED TO DO***
    End Sub

    Sub CreateDistLookup()

        Dim DistLookupFile As FileStream
        Dim dl As StreamReader
        Dim LookupLine As String
        Dim LookupArray() As String

        If DictCreated = False Then
            DistLookupFile = New FileStream(DirPath & "DistrictITRCZoneLookup.csv", FileMode.Open, FileAccess.Read)
            dl = New StreamReader(DistLookupFile, System.Text.Encoding.Default)
            'read header row
            LookupLine = dl.ReadLine()
            'read first row
            LookupLine = dl.ReadLine()
            Do Until LookupLine Is Nothing
                LookupArray = Split(LookupLine, ",")
                DistZoneLookup.Add(LookupArray(0), LookupArray(2))
                LookupLine = dl.ReadLine()
            Loop
            DictCreated = True
            dl.Close()
        End If

    End Sub

    Sub DistrictLookup()
        Dim District As String
        Dim ZoneNum As Long
        Dim value As Long
        Dim ExtraPop As Double

        District = PopArray(3)

        If DistZoneLookup.TryGetValue(District, value) Then
            ZoneNum = value
        Else
            logarray(logNum, 0) = "No ITRC Zone found in lookup table for District " & District & ".  Model run terminated."
            logNum += 1
            Call WriteData("Logfile", "", logarray)
            zp.Close()
            End
        End If

        If ZoneNum > 0 Then
            ExtraPop = PopArray(4)
            ZonePops(ZoneNum) += ExtraPop
        End If

    End Sub

    Sub GORLookup()
        Dim GOR As String
        Dim ExtraPop As Double

        GOR = GORArray(3)
        ExtraPop = GORArray(4)

        Select Case GOR
            Case "Scotland"
                If SWDisagg = True Then
                    ZonePops(145) += ExtraPop
                End If
            Case "Wales"
                If SWDisagg = True Then
                    ZonePops(146) += ExtraPop
                End If
            Case "East Midlands"
                ZonePops(147) += ExtraPop
            Case "East Of England"
                ZonePops(148) += ExtraPop
            Case "East of England"
                ZonePops(148) += ExtraPop
            Case "London"
                ZonePops(149) += ExtraPop
            Case "North East"
                ZonePops(150) += ExtraPop
            Case "North West"
                ZonePops(151) += ExtraPop
            Case "South East"
                ZonePops(152) += ExtraPop
            Case "South West"
                ZonePops(153) += ExtraPop
            Case "West Midlands"
                ZonePops(154) += ExtraPop
            Case "Yorkshire and The Humber"
                ZonePops(155) += ExtraPop
            Case "Yorkshire and the Humber"
                ZonePops(155) += ExtraPop
            Case Else
        End Select
    End Sub

    Sub EcoRegionLookup()
        Dim region As String
        Dim regionfound As Boolean


        region = EcoArray(1)
        Select Case region
            Case "Scotland"
                GorGVA(0) += EcoArray(3)
            Case "Wales"
                GorGVA(1) += EcoArray(3)
            Case "North East"
                GorGVA(2) += EcoArray(3)
            Case "North West"
                GorGVA(3) += EcoArray(3)
            Case "West Midlands"
                GorGVA(4) += EcoArray(3)
            Case "Yorkshire and The Humber"
                GorGVA(5) += EcoArray(3)
            Case "Yorkshire and the Humber"
                GorGVA(5) += EcoArray(3)
            Case "South West"
                GorGVA(6) += EcoArray(3)
            Case "East Midlands"
                GorGVA(7) += EcoArray(3)
            Case "South East"
                GorGVA(8) += EcoArray(3)
            Case "East of England"
                GorGVA(9) += EcoArray(3)
            Case "East Of England"
                GorGVA(9) += EcoArray(3)
            Case "London"
                GorGVA(10) += EcoArray(3)
            Case "Northern Ireland"
                GorGVA(11) += EcoArray(3)
            Case Else
                For v = 0 To 10
                    If badregions(v) = region Then
                        regionfound = True
                        Exit For
                    End If
                Next
                If regionfound = False Then
                    If badregioncount < 10 Then
                        badregions(badregioncount) = region
                        MsgBox("Unrecognised region " & EcoArray(1) & " included in input gva file.  These rows have been excluded from the output data.")
                        badregioncount += 1
                    Else
                        MsgBox("More than 10 unrecognised regions included in input gva file.  Model run terminated.")
                        logarray(logNum, 0) = "More than 10 unrecognised regions included in input gva file.  Model run terminated."
                        logNum += 1
                        Call WriteData("Logfile", "", logarray)
                        End
                    End If
                End If

        End Select
    End Sub

    Sub AssignZoneGVA()

        Select Case ZoneID
            'GOR0 = Scotland, 1 = Wales, 2 = North East, 3 = North West, 4 = West Midlands, 5 = Yorkshire, 6 = South West, 7 = E Midlands, 8 = South East, 9 = E England, 10 = London, 11 = NI 
            Case 1
                ZoneGVA = GorGVA(0) * 0.060455631
            Case 2
                ZoneGVA = GorGVA(0) * 0.068447693
            Case 3
                ZoneGVA = GorGVA(0) * 0.018235227
            Case 4
                ZoneGVA = GorGVA(0) * 0.011879807
            Case 5
                ZoneGVA = GorGVA(6) * 0.038194505
            Case 6
                ZoneGVA = GorGVA(9) * 0.023621868
            Case 7
                ZoneGVA = GorGVA(3) * 0.01349894
            Case 8
                ZoneGVA = GorGVA(3) * 0.01052078
            Case 9
                ZoneGVA = GorGVA(1) * 0.016514465
            Case 10
                ZoneGVA = GorGVA(6) * 0.037030341
            Case 11
                ZoneGVA = GorGVA(8) * 0.019961322
            Case 12
                ZoneGVA = GorGVA(1) * 0.04350625
            Case 13
                ZoneGVA = GorGVA(8) * 0.029428438
            Case 14
                ZoneGVA = GorGVA(6) * 0.115945541
            Case 15
                ZoneGVA = GorGVA(8) * 0.060800016
            Case 16
                ZoneGVA = GorGVA(1) * 0.041793185
            Case 17
                ZoneGVA = GorGVA(9) * 0.123092298
            Case 18
                ZoneGVA = GorGVA(1) * 0.161277952
            Case 19
                ZoneGVA = GorGVA(1) * 0.048927055
            Case 20
                ZoneGVA = GorGVA(9) * 0.037489432
            Case 21
                ZoneGVA = GorGVA(1) * 0.020821752
            Case 22
                ZoneGVA = GorGVA(3) * 0.04719458
            Case 23
                ZoneGVA = GorGVA(3) * 0.042459555
            Case 24
                ZoneGVA = GorGVA(0) * 0.006676219
            Case 25
                ZoneGVA = GorGVA(1) * 0.030972691
            Case 26
                ZoneGVA = GorGVA(6) * 0.073227266
            Case 27
                ZoneGVA = GorGVA(3) * 0.050402639
            Case 28
                ZoneGVA = GorGVA(2) * 0.012608497
            Case 29
                ZoneGVA = GorGVA(1) * 0.027006846
            Case 30
                ZoneGVA = GorGVA(7) * 0.070449109
            Case 31
                ZoneGVA = GorGVA(7) * 0.138778729
            Case 32
                ZoneGVA = GorGVA(6) * 0.127213416
            Case 33
                ZoneGVA = GorGVA(6) * 0.064313764
            Case 34
                ZoneGVA = GorGVA(0) * 0.020656821
            Case 35
                ZoneGVA = GorGVA(0) * 0.02379153
            Case 36
                ZoneGVA = GorGVA(2) * 0.039571279
            Case 37
                ZoneGVA = GorGVA(0) * 0.014413537
            Case 38
                ZoneGVA = GorGVA(0) * 0.013022429
            Case 39
                ZoneGVA = GorGVA(0) * 0.012056282
            Case 40
                ZoneGVA = GorGVA(0) * 0.013364949
            Case 41
                ZoneGVA = GorGVA(5) * 0.02579172
            Case 42
                ZoneGVA = GorGVA(8) * 0.038461445
            Case 43
                ZoneGVA = GorGVA(0) * 0.164664127
            Case 44
                ZoneGVA = GorGVA(0) * 0.003697082
            Case 45
                ZoneGVA = GorGVA(9) * 0.218326045
            Case 46
                ZoneGVA = GorGVA(0) * 0.024825203
            Case 47
                ZoneGVA = GorGVA(0) * 0.048158495
            Case 48
                ZoneGVA = GorGVA(1) * 0.054486648
            Case 49
                ZoneGVA = GorGVA(0) * 0.169333934
            Case 50
                ZoneGVA = GorGVA(6) * 0.120203255
            Case 51
                ZoneGVA = GorGVA(10)
            Case 52
                ZoneGVA = GorGVA(3) * 0.294162873
            Case 53
                ZoneGVA = GorGVA(1) * 0.037840969
            Case 54
                ZoneGVA = GorGVA(3) * 0.016975339
            Case 55
                ZoneGVA = GorGVA(8) * 0.148459342
            Case 56
                ZoneGVA = GorGVA(2) * 0.009033479
            Case 57
                ZoneGVA = GorGVA(4) * 0.015396978
            Case 58
                ZoneGVA = GorGVA(9) * 0.236364725
            Case 59
                ZoneGVA = GorGVA(0) * 0.030361449
            Case 60
                ZoneGVA = GorGVA(0) * 0.011916458
            Case 61
                ZoneGVA = GorGVA(1) * 0.017786226
            Case 62
                ZoneGVA = GorGVA(8) * 0.00968305
            Case 63
                ZoneGVA = GorGVA(6) * 0.000287273
            Case 64
                ZoneGVA = GorGVA(8) * 0.135341045
            Case 65
                ZoneGVA = GorGVA(5) * 0.023032198
            Case 66
                ZoneGVA = GorGVA(3) * 0.117011832
            Case 67
                ZoneGVA = GorGVA(7) * 0.076581801
            Case 68
                ZoneGVA = GorGVA(7) * 0.155919456
            Case 69
                ZoneGVA = GorGVA(7) * 0.127945286
            Case 70
                ZoneGVA = GorGVA(9) * 0.040137089
            Case 71
                ZoneGVA = GorGVA(8) * 0.019304451
            Case 72
                ZoneGVA = GorGVA(3) * 0.125359422
            Case 73
                ZoneGVA = GorGVA(1) * 0.01495659
            Case 74
                ZoneGVA = GorGVA(2) * 0.013114611
            Case 75
                ZoneGVA = GorGVA(0) * 0.010028354
            Case 76
                ZoneGVA = GorGVA(8) * 0.038196045
            Case 77
                ZoneGVA = GorGVA(1) * 0.038390902
            Case 78
                ZoneGVA = GorGVA(0) * 0.013801668
            Case 79
                ZoneGVA = GorGVA(1) * 0.044411283
            Case 80
                ZoneGVA = GorGVA(1) * 0.061573603
            Case 81
                ZoneGVA = GorGVA(9) * 0.12518887
            Case 82
                ZoneGVA = GorGVA(0) * 0.016212232
            Case 83
                ZoneGVA = GorGVA(5) * 0.014957816
            Case 84
                ZoneGVA = GorGVA(0) * 0.048054506
            Case 85
                ZoneGVA = GorGVA(5) * 0.01533818
            Case 86
                ZoneGVA = GorGVA(6) * 0.045102248
            Case 87
                ZoneGVA = GorGVA(5) * 0.05419694
            Case 88
                ZoneGVA = GorGVA(7) * 0.173352092
            Case 89
                ZoneGVA = GorGVA(2) * 0.024148542
            Case 90
                ZoneGVA = GorGVA(7) * 0.098694676
            Case 91
                ZoneGVA = GorGVA(7) * 0.149001078
            Case 92
                ZoneGVA = GorGVA(0) * 0.002980279
            Case 93
                ZoneGVA = GorGVA(8) * 0.079318833
            Case 94
                ZoneGVA = GorGVA(1) * 0.031706464
            Case 95
                ZoneGVA = GorGVA(0) * 0.02537279
            Case 96
                ZoneGVA = GorGVA(9) * 0.037518825
            Case 97
                ZoneGVA = GorGVA(6) * 0.043659116
            Case 98
                ZoneGVA = GorGVA(6) * 0.031302864
            Case 99
                ZoneGVA = GorGVA(8) * 0.023297064
            Case 100
                ZoneGVA = GorGVA(1) * 0.036637372
            Case 101
                ZoneGVA = GorGVA(8) * 0.026420908
            Case 102
                ZoneGVA = GorGVA(2) * 0.012654127
            Case 103
                ZoneGVA = GorGVA(0) * 0.025430736
            Case 104
                ZoneGVA = GorGVA(1) * 0.062914347
            Case 105
                ZoneGVA = GorGVA(7) * 0.009277772
            Case 106
                ZoneGVA = GorGVA(0) * 0.013619148
            Case 107
                ZoneGVA = GorGVA(0) * 0.004096813
            Case 108
                ZoneGVA = GorGVA(4) * 0.024165576
            Case 109
                ZoneGVA = GorGVA(8) * 0.022462912
            Case 110
                ZoneGVA = GorGVA(6) * 0.08749088
            Case 111
                ZoneGVA = GorGVA(0) * 0.019061387
            Case 112
                ZoneGVA = GorGVA(6) * 0.056282164
            Case 113
                ZoneGVA = GorGVA(0) * 0.046345978
            Case 114
                ZoneGVA = GorGVA(5) * 0.11251364
            Case 115
                ZoneGVA = GorGVA(8) * 0.026411524
            Case 116
                ZoneGVA = GorGVA(9) * 0.023619401
            Case 117
                ZoneGVA = GorGVA(4) * 0.067587075
            Case 118
                ZoneGVA = GorGVA(0) * 0.015433111
            Case 119
                ZoneGVA = GorGVA(2) * 0.019036597
            Case 120
                ZoneGVA = GorGVA(4) * 0.019960246
            Case 121
                ZoneGVA = GorGVA(9) * 0.112550414
            Case 122
                ZoneGVA = GorGVA(8) * 0.158169919
            Case 123
                ZoneGVA = GorGVA(1) * 0.078896274
            Case 124
                ZoneGVA = GorGVA(6) * 0.058066529
            Case 125
                ZoneGVA = GorGVA(4) * 0.016737958
            Case 126
                ZoneGVA = GorGVA(1) * 0.059102152
            Case 127
                ZoneGVA = GorGVA(9) * 0.022091033
            Case 128
                ZoneGVA = GorGVA(6) * 0.017879231
            Case 129
                ZoneGVA = GorGVA(1) * 0.021850279
            Case 130
                ZoneGVA = GorGVA(2) * 0.123945192
            Case 131
                ZoneGVA = GorGVA(3) * 0.028301717
            Case 132
                ZoneGVA = GorGVA(4) * 0.058349044
            Case 133
                ZoneGVA = GorGVA(8) * 0.02638664
            Case 134
                ZoneGVA = GorGVA(0) * 0.011279465
            Case 135
                ZoneGVA = GorGVA(0) * 0.03232666
            Case 136
                ZoneGVA = GorGVA(4) * 0.258746361
            Case 137
                ZoneGVA = GorGVA(8) * 0.08490102
            Case 138
                ZoneGVA = GorGVA(5) * 0.222696895
            Case 139
                ZoneGVA = GorGVA(6) * 0.08380161
            Case 140
                ZoneGVA = GorGVA(8) * 0.02503304
            Case 141
                ZoneGVA = GorGVA(8) * 0.027962985
            Case 142
                ZoneGVA = GorGVA(4) * 0.047552524
            Case 143
                ZoneGVA = GorGVA(1) * 0.048626694
            Case 144
                ZoneGVA = GorGVA(5) * 0.022976849
            Case 145
                ZoneGVA = GorGVA(0)
            Case 146
                ZoneGVA = GorGVA(1)
            Case 147
                ZoneGVA = GorGVA(7)
            Case 148
                ZoneGVA = GorGVA(9)
            Case 149
                ZoneGVA = GorGVA(10)
            Case 150
                ZoneGVA = GorGVA(2)
            Case 151
                ZoneGVA = GorGVA(3)
            Case 152
                ZoneGVA = GorGVA(8)
            Case 153
                ZoneGVA = GorGVA(6)
            Case 154
                ZoneGVA = GorGVA(4)
            Case 155
                ZoneGVA = GorGVA(5)
        End Select
    End Sub


    Function get_population_data_by_economics_scenario_tr_zone(ByVal scenario As String, ByVal year As Integer, ByVal link_type As String, ByVal ZoneID As Integer)
        Dim theSQL As String = ""
        Dim InputArray(,) As String = Nothing

        'The database function seems to be brocken so it will return a temporary result until it's fixed
        Return 5000

        theSQL = "SELECT * FROM cdam_get_population_data_by_population_scenario_od_tr_zone(" & scenario & "," & year & "'" & link_type & "')" & " WHERE zone_id = " & ZoneID
        theSQL &= " AS (scenario_id varchar, year integer, gender varchar, category varchar, " & Chr(34) & "DistrictName" & Chr(34) & " varchar, zone_id integer, " & Chr(34) & "ZoneName" & Chr(34) & " varchar, district_code varchar, value double precision);"

        If LoadSQLDataToArray(InputArray, theSQL) = False Then
            InputArray = Nothing
            Return False
        Else
            Return 0
        End If


    End Function


    Function get_regional_gva_data_by_economics_scenario_tr_zone(ByVal scenario As String, ByVal year As Integer, ByVal link_type As String, ByVal ZoneID As Integer)
        Dim theSQL As String = ""
        Dim InputArray(,) As String = Nothing

        'The database function seems to be brocken so it will return a temporary result until it's fixed
        Return 10000

        theSQL = "SELECT * FROM cdam_get_regional_gva_data_by_economics_scenario_tr_zone(" & scenario & "," & year & "'" & link_type & "')" & " WHERE zone_id = " & ZoneID

        If LoadSQLDataToArray(InputArray, theSQL) = False Then
            InputArray = Nothing
            Return False
        Else
            Return 0
        End If


    End Function

    Sub get_zone_by_flowid(ByVal FlowID As Integer, ByRef Zone1ID As Integer, ByRef Zone2ID As Integer)
        Dim theSQL As String = ""
        Dim InputArray(,) As String = Nothing

        theSQL = "SELECT * TR_LU_RoadInputFlows where id =" & FlowID

        If LoadSQLDataToArray(InputArray, theSQL) = True Then
            Zone1ID = InputArray(0, 1)
            Zone2ID = InputArray(0, 1)
        End If

    End Sub

    '****************************************************************************************
    ' Function: ReadData 
    '
    ' Purpose: Get an array of data from a csv file - to be replaced with database calls
    ' 
    ' Parameters:   Type - type of data (e.g. Road, Rail)
    '               SubType - subtype of data
    '               Inputrray - array of data to be output
    '               ModelRunID - used in most WHERE clauses
    '               IsInitialYear - TRUE - read from initial file, FALSE - read from temp file
    '               Year - current year of the calculation, this is for external variable file to read from the correct line
    '               datatype - variant type of data in array (e.g. string, integer)
    '               Connection - file path - to be replaced with database connection string
    '               whereID - an integer to be used in the WHERE clause of the SQL
    '****************************************************************************************

    Function ReadData(ByVal Type As String, ByVal SubType As String, ByRef InputArray(,) As String,
                       ByVal ModelRunID As Integer, Optional ByVal IsInitialYear As Boolean = True,
                       Optional ByVal Year As Integer = 0, Optional ByVal datatype As VariantType = VariantType.String,
                       Optional Connection As String = "", Optional whereID As Integer = 0) As Boolean
        Dim TheFileName As String = ""
        Dim DataFile As FileStream
        Dim DataRead As StreamReader
        Dim dbheadings As String
        Dim dbline As String
        Dim dbarray() As String
        Dim iR As Integer = 0, iC As Integer = 0
        Dim DataRows As Integer = 0, DataColumns As Integer = 0
        Dim theSQL As String = ""


        'Check if file path has been selected - if not then use default.
        If Connection = "" Then
            Connection = DirPath
        End If
        'Make sure the file path ends with at \
        If Connection.Substring(Len(Connection) - 1, 1) <> "\" Then
            Connection = Connection & "\"
        End If

        'Get the filename of datafile based on Type and SubType
        'Get the initial input data file if it is year 1, otherwise get the temp file
        'the size of the array must be correct
        Select Case Type
            Case "System"
                Select Case SubType
                    Case "ModelRunDetails"
                        theSQL = "SELECT * FROM " & Chr(34) & "ISL_ModelRuns" & Chr(34) & " WHERE modelrun_id = " & ModelRunID
                End Select
            Case "Scenario"
                Select Case SubType
                    Case "Pop by Zone"

                End Select
            Case "RoadZone"
                Select Case SubType
                    Case "Input"
                        If IsInitialYear = True Then
                            TheFileName = "RoadZoneInputData2010.csv"
                        ElseIf IsInitialYear = False Then
                            TheFileName = FilePrefix & "RoadZoneTemp.csv"
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "RoadZoneExtVar" & EVFileSuffix & ".csv"
                    Case "NewCap"
                        TheFileName = CapFilePrefix & "RoadZoneCapChange.csv"
                    Case "CapChange"
                        TheFileName = FilePrefix & "RoadZoneCapChange.csv"
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\RoadZoneElasticities.csv"
                End Select
            Case "RoadLink"
                Select Case SubType
                    Case "Input"
                        If IsInitialYear = True Then
                            TheFileName = "RoadInputData2010.csv"
                        ElseIf IsInitialYear = False Then
                            TheFileName = FilePrefix & "RoadLinkTemp.csv"
                        End If
                    Case "Temp Annual"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadLink_Annual" & Chr(34) & " WHERE modelrun_id = " & ModelRunID & "year =" & Year
                    Case "Temp Hourly"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadLink_Hourly" & Chr(34) & " WHERE modelrun_id = " & ModelRunID & "year =" & Year
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "ExternalVariables" & EVFileSuffix & ".csv"
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\RoadLinkElasticities.csv"
                    Case "FreeFlowSpeed"
                        'A header has been added to the original file to keep in the same format
                        TheFileName = "FreeFlowSpeedsv0.7.csv"
                    Case "DailyProfile"
                        'A header has been added to the original file to keep in the same format
                        TheFileName = "DailyTripProfile.csv"
                End Select
            Case "RailZone"
                Select Case SubType
                    Case "Input"
                        If IsInitialYear = True Then
                            TheFileName = "RailZoneInputData2010.csv"
                        ElseIf IsInitialYear = False Then
                            TheFileName = FilePrefix & "RailZoneTemp.csv"
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "RailZoneExtVar" & EVFileSuffix & ".csv"
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "RailZoneCapChange.csv"
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\RailZoneElasticities.csv"
                    Case "ElSchemes"
                        TheFileName = EVFilePrefix & "RailZoneElectrificationDates.csv"
                    Case "EVScale"
                        TheFileName = "RailZoneEVScaling.csv"
                End Select
            Case "RailLink"
                Select Case SubType
                    Case "Input"
                        If IsInitialYear = True Then
                            TheFileName = "RailLinkInputData2010.csv"
                        ElseIf IsInitialYear = False Then
                            TheFileName = FilePrefix & "RailLinkTemp.csv"
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "RailLinkExtVar" & EVFileSuffix & ".csv"
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "RailLinkCapChange.csv"
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\RailLinkElasticities.csv"
                    Case "ElSchemes"
                        TheFileName = EVFilePrefix & "RailLinkElectrificationDates.csv"
                    Case "EVScale"
                        TheFileName = "RailLinkEVScaling.csv"
                    Case "OldRlEl"
                        TheFileName = "RailElectrificationSchemes.csv"
                    Case "OldRzEl"
                        TheFileName = "RailZoneElectrificationSchemes.csv"
                End Select
            Case "Seaport"
                Select Case SubType
                    Case "Input"
                        If IsInitialYear = True Then
                            TheFileName = "SeaFreightInputData.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_I_SeaFreight_Base" & Chr(34) & " WHERE year = " & 2010
                        ElseIf IsInitialYear = False Then
                            TheFileName = FilePrefix & "SeaTemplate.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_I_SeaFreight_Run" & Chr(34) & " WHERE modelrun_id = " & ModelRunID
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "SeaFreightExtVar" & EVFileSuffix & ".csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_O_SeaFreightExternalVariables" & Chr(34) & " WHERE modelrun_id = " & ModelRunID
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "SeaFreightCapChange.csv"
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\SeaFreightElasticities.csv"
                    Case Else
                        'for error handling
                End Select
            Case "AirNode"
                Select Case SubType
                    Case "Input"
                        If IsInitialYear = True Then
                            TheFileName = "AirNodeInputData2010.csv"
                        ElseIf IsInitialYear = False Then
                            TheFileName = FilePrefix & "AirNodeTemp.csv"
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "AirNodeExtVar" & EVFileSuffix & ".csv"
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "AirNodeCapChange.csv"
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\AirElasticities.csv"
                End Select
            Case "AirFlow"
                Select Case SubType
                    Case "Input"
                        If IsInitialYear = True Then
                            TheFileName = "AirFlowInputData2010.csv"
                        ElseIf IsInitialYear = False Then
                            TheFileName = FilePrefix & "AirFlowTemp.csv"
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "AirFlowExtVar.csv"
                End Select
            Case "SubStrategy"
                TheFileName = "CommonVariablesTR" & SubStrategy & ".csv"
            Case "Energy"
                Connection = DBaseEneFile
                TheFileName = ""
            Case Else
                'for error handling
        End Select



        'If there is SQL then get the data from the database - otherwise from the csv file
        If theSQL <> "" Then
            'Load data returned by the SQL into the InputArray
            If LoadSQLDataToArray(InputArray, theSQL) = False Then
                InputArray = Nothing
                Return False
            End If

        Else
            'Else get the data from the file and parse it into an array
            Try
                DataFile = New FileStream(Connection & TheFileName, IO.FileMode.Open, IO.FileAccess.Read)
            Catch exIO As IOException
                MsgBox("An error was encountered trying to access the file " & Connection & TheFileName)
            End Try
            DataRead = New IO.StreamReader(DataFile, System.Text.Encoding.Default)

            'read header row
            dbheadings = DataRead.ReadLine
            dbarray = Split(dbheadings, ",")
            'Get a line of data from file
            dbline = DataRead.ReadLine
            dbarray = Split(dbline, ",")

            'read to the correct line
            If SubType = "ExtVar" Then
                Do
                    'if it is the current year line then stop
                    If dbarray(0) = Year Then
                        Exit Do
                    End If
                    'if not, continue to read line
                    dbline = DataRead.ReadLine
                    dbarray = Split(dbline, ",")
                Loop

            End If

            'loop through row to get data
            For iR = 1 To UBound(InputArray, 1)
                If dbline Is Nothing Then
                    Exit For
                End If
                For iC = 0 To UBound(InputArray, 2)
                    InputArray(iR, iC) = UnNull(dbarray(iC).ToString, VariantType.Char)
                    'InputArray(iR, iC) = dbarray(iC)
                Next
                dbline = DataRead.ReadLine
                dbarray = Split(dbline, ",")
            Next

            DataRead.Close()

            If IsInitialYear = False Then
                'delete the temp file to recreate for current year
                System.IO.File.Delete(Connection & TheFileName)
            End If


        End If


        Return True

    End Function

    Public Function LoadSQLDataToArray(ByRef aryData(,) As String, ByVal theSQL As String) As Boolean
        Dim iR As Integer
        Dim iC As Integer
        Dim strSampleOutput As String = ""
        Dim DataRows As Integer
        Dim DataColumns As Integer
        Dim TableData As DataTable

        Try
            ConnectToDBase()
            cmd.Connection = m_conn
            cmd.CommandText = theSQL

            Dim da As Odbc.OdbcDataAdapter = New Odbc.OdbcDataAdapter(cmd)
            Dim ds As New DataSet
            da.Fill(ds, "Data")

            TableData = ds.Tables(0)

            DataRows = TableData.Rows.Count
            DataColumns = TableData.Columns.Count

            'If there is no data then just exit
            If DataRows = 0 Then
                Return False
            End If

            'Store column names in the 0 row
            ReDim aryData(DataRows, DataColumns - 1)
            For iC = 0 To DataColumns - 1
                aryData(0, iC) = Trim(CStr(TableData.Columns(iC).ColumnName))
            Next
            'Load Data into the array
            For iR = 0 To DataRows - 1

                For iC = 0 To DataColumns - 1
                    aryData(iR + 1, iC) = Trim(CStr(UnNull(TableData.Rows(iR).Item(iC), VariantType.Char)))
                Next
            Next

            Return True

        Catch ex As Exception
            Throw ex
            Return False
        End Try

    End Function

    '****************************************************************************************
    ' Function: WriteData 
    '
    ' Purpose: Output an array to a csv file - to be replaced with database calls
    ' 
    ' Parameters:   Type - type of data (e.g. Road, Rail)
    '               SubType - subtype of data
    '               OutputArray - array of data to be output
    '               TemoArray - array of temp data to be output
    '               IsNewFile_IsInsert - TRUE - create a new file OR insert data to table, 
    '                                    FALSE - update and existing file OR update data to table
    '               Connection - file path - to be replaced with database connection string
    '****************************************************************************************


    'TODO add the log file writedata
    Function WriteData(ByVal Type As String, ByVal SubType As String, ByRef OutputArray(,) As String,
                       Optional ByVal TempArray(,) As String = Nothing,
                       Optional ByVal IsNewFile_IsInsert As Boolean = True,
                       Optional Connection As String = "") As Boolean

        Dim OutFileName As String = "", TempFileName As String = ""
        Dim TempFile As IO.FileStream
        Dim TempWrite As StreamWriter
        Dim OutputFile As IO.FileStream
        Dim OutputWrite As StreamWriter
        Dim OutputRead As StreamReader
        Dim Line As String = ""
        Dim ix As Integer, iy As Integer
        Dim header As String
        Dim tempheader As String
        Dim aryFieldNames As New ArrayList
        Dim aryFieldValues As New ArrayList
        Dim ToSQL As Boolean = False
        Dim TempTableName As String = ""
        Dim TableName As String = ""

        'Check if file path has been selected - if not then use default.
        If Connection = "" Then
            Connection = DirPath
        End If
        'Make sure the file path ends with at \
        If Connection.Substring(Len(Connection) - 1, 1) <> "\" Then
            Connection = Connection & "\"
        End If

        'Get the filename of datafile based on Type and SubType
        'TODO - replace with database calls
        Select Case Type
            Case "RoadZone"
                Select Case SubType
                    Case "Output"
                        OutFileName = FilePrefix & "RoadZoneOutputData.csv"
                        TempFileName = FilePrefix & "RoadZoneTemp.csv"
                        header = "Yeary,ZoneID,Vkmy,Spdy,Petroly,Diesely,Electricy,LPGy,CNGy,Hydrogeny,VKmMwayy,VkmRurAy,VkmRurMiny,VkmUrby,SpdMWayy,SpdRurAy,SpdRurMiny,SpdUrby,VkmPet,VkmDie,VkmPH,VkmDH,VkmPEH,VkmE,VkmLPG,VkmCNG,VkmHyd,VkmFC"
                        tempheader = "Yeary,ZoneID,PopZ,GVAZ,Speed,CarCost,LGVCost,HGV1Cost,HGV2Cost,PSVCost,Vkm,LKmMway,LkmRural,LKmRurMin,LKmUrban,RKmMway,RkmRural,RKmRurMin,RKmUrban,VKmMway,RVCatTraf(1 - 1),RVCatTraf(1 - 2),RVCatTraf(1 - 3),RVCatTraf(1 - 4),RVCatTraf(1 - 5),VKmRurA,RVCatTraf(2 - 1),RVCatTraf(2 - 2),RVCatTraf(2 - 3),RVCatTraf(2 - 4),RVCatTraf(2 - 5),VKmRurMin,RVCatTraf(3 - 1),RVCatTraf(3 - 2),RVCatTraf(3 - 3),RVCatTraf(3 - 4),RVCatTraf(3 - 5),VKmUrb,RVCatTraf(4 - 1),RVCatTraf(4 - 2),RVCatTraf(4 - 3),RVCatTraf(4 - 4),RVCatTraf(4 - 5),SuppressedTraffic(1 - 1),SuppressedTraffic(1 - 2),SuppressedTraffic(1 - 3),SuppressedTraffic(1 - 4),SuppressedTraffic(2 - 1),SuppressedTraffic(2 - 2),SuppressedTraffic(2 - 3),SuppressedTraffic(2 - 4),SuppressedTraffic(3 - 1),SuppressedTraffic(3 - 2),SuppressedTraffic(3 - 3),SuppressedTraffic(3 - 4),SuppressedTraffic(4 - 1),SuppressedTraffic(4 - 2),SuppressedTraffic(4 - 3),SuppressedTraffic(4 - 4),SpdMWayy,SpdRurAy,SpdRurMiny,SpdUrby,"
                    Case "ExtVar"
                        OutFileName = EVFilePrefix & "RoadZoneExtVar.csv"
                        header = "Yeary,ZoneID,PopZy,GVAZy,Costy,LaneKm,LKmMway,LKmRurAD,LKmRurAS,LKmRurMin,LKmUrbD,LKmUrbS,PCar,DCar,ECar,PLGV,DLGV,ELGV,DHGV,EHGV,DPSV,EPSV,PBike,EBike,FCBike,PHCar,DHCar,PECar,HCar,FCCar,DHLGV,PELGV,LLGV,CLGV,DHPSV,PEPSV,LPSV,CPSV,FCPSV,DHHGV,HHGV,FCHGV"
                    Case "NewCap"
                        OutFileName = EVFilePrefix & "RoadZoneCapChange.csv"
                        header = "ZoneID,ChangeYear,MWayLaneKmCh,RurADLaneKmCh,RurASLaneKmCh,RurMLaneKmCh,UrbDLaneKmCh,UrbSLaneKmCh"
                    Case "RoadZoneNewCap"
                        OutFileName = FilePrefix & "RoadZoneNewCap.csv"
                        header = "ZoneID,Yeary,MWayCap,RurACap,RurMinCap,UrbCap"
                    Case "RoadZoneFuel"
                        OutFileName = FilePrefix & "RoadZoneFuelConsumption.csv"
                        header = "ZoneID,Yeary,PetCary,PetLGVy,DieCary,DieLGVy,DieHGV23y,DieHGV4y,DiePSVy,EleCary,EleLGVy,ElePSVy,LPGLGVy,LPGPSVy,CNGLGVy,CNGPSVy,HydCary,HydHGV23y,HydHGV4y,HydPSVy"
                End Select
            Case "RoadLink"
                Select Case SubType
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_RoadOutputFlows"
                        'header = "Yeary,FlowID,PCUTotal,SpeedMean,PCUMway,PCUDual,PCUSing,SpdMway,SpdDual,SpdSing,MSC1,MSC2,MSC3,MSC4,MSC5,MSC6,DSC1,DSC2,DSC3,DSC4,DSC5,DSC6,SSC1,SSC2,SSC3,SSC4,SSC5,SSC6,SSC7,SSC8,SpdMSC1,SpdMSC2,SpdMSC3,SpdMSC4,SpdMSC5,SpdMSC6,SpdDSC1,SpdDSC2,SpdDSC3,SpdDSC4,SpdDSC5,SpdDSC6,SpdSSC1,SpdSSC2,SpdSSC3,SpdSSC4,SpdSSC5,SpdSSC6,SpdSSC7,SpdSSC8,MWayLatent,DualLatent,SingLatent,MFullHrs,DFullHrs,SFullHrs,CostMway,CostDual,CostSing"
                        header = "modelrun_id, FlowID, year,PCUTotal,SpeedMean,PCUMway,PCUDual,PCUSing,SpdMway,SpdDual,SpdSing,MSC1,MSC2,MSC3,MSC4,MSC5,MSC6,DSC1,DSC2,DSC3,DSC4,DSC5,DSC6,SSC1,SSC2,SSC3,SSC4,SSC5,SSC6,SSC7,SSC8,SpdMSC1,SpdMSC2,SpdMSC3,SpdMSC4,SpdMSC5,SpdMSC6,SpdDSC1,SpdDSC2,SpdDSC3,SpdDSC4,SpdDSC5,SpdDSC6,SpdSSC1,SpdSSC2,SpdSSC3,SpdSSC4,SpdSSC5,SpdSSC6,SpdSSC7,SpdSSC8,MWayLatent,DualLatent,SingLatent,MFullHrs,DFullHrs,SFullHrs,CostMway,CostDual,CostSing"
                    Case "Temp Annual"
                        ToSQL = True
                        TableName = "TR_IO_RoadLink_Annual"
                        header = "modelrun_id, year,FlowID,MLanes,DLanes,SLanes,MLanesNew,DLanesNew,SLanesNew,msc1,msc2,msc3,msc4,msc5,msc6,dsc1,dsc2,dsc3,dsc4,dsc5,dsc6,ssc1,ssc2,ssc3,ssc4,ssc5,ssc6,ssc7,ssc8,MaxCapM,MaxCapD,MaxCapS,AddedLane0,AddedLane1,AddedLane2"
                    Case "Temp Hourly"
                        ToSQL = True
                        TableName = "TR_IO_RoadLink_Hourly"
                        header = "modelrun_id, year,FlowID,HourID, MRoadTypeFlows,DRoadTypeFlows,SRoadTypeFlows,"
                        For x = 1 To 6
                            header = header & "M" & x & "HourlyFlows" & "," & "M" & x & "Charge" & "," & "M" & x & "LatentFlows" & "," & "M" & x & "NewHourlySpeeds" & ","
                        Next
                        For x = 1 To 6
                            header = header & "D" & x & "HourlyFlows" & "," & "D" & x & "Charge" & "," & "D" & x & "LatentFlows" & "," & "D" & x & "NewHourlySpeeds" & ","
                        Next
                        For x = 1 To 8
                            header = header & "S" & x & "HourlyFlows" & "," & "S" & x & "Charge" & "," & "S" & x & "LatentFlows" & "," & "S" & x & "NewHourlySpeeds" & ","
                        Next
                    Case "Temp"
                        TempFileName = FilePrefix & "RoadLinkTemp.csv"
                        header = "Yeary,FlowID,MLanes,DLanes,SLanes,MLanesNew,DLanesNew,SLanesNew,MSC1,MSC2,MSC3,MSC4,MSC5,MSC6,DSC1,DSC2,DSC3,DSC4,DSC5,DSC6,SSC1,SSC2,SSC3,SSC4,SSC5,SSC6,SSC7,SSC8,PopZ1,PopZ2,GVAZ1,GVAZ2,"
                        'For x = 1 To 6
                        'For c = 0 To 23
                        'header = header & "M" & x & "Cost" & c & ","
                        'Next
                        'Next
                        'For x = 1 To 6
                        'For c = 0 To 23
                        'header = header & "D" & x & "Cost" & c & ","
                        'Next
                        'Next
                        'For x = 1 To 8
                        'For c = 0 To 23
                        'header = header & "S" & x & "Cost" & c & ","
                        'Next
                        'Next
                        header = header & "MaxCapM,MaxCapD,MaxCapS,AddedLane0,AddedLane1,AddedLane2,"
                        For x = 1 To 6
                            For c = 0 To 23
                                header = header & "M" & x & "HourlyFlows" & c & "," & "MRoadTypeFlows" & c & "," & "M" & x & "Charge" & c & "," & "M" & x & "LatentFlows" & c & "," & "M" & x & "NewHourlySpeeds" & c & ","
                            Next
                        Next
                        For x = 1 To 6
                            For c = 0 To 23
                                header = header & "D" & x & "HourlyFlows" & c & "," & "DRoadTypeFlows" & c & "," & "D" & x & "Charge" & c & "," & "D" & x & "LatentFlows" & c & "," & "D" & x & "NewHourlySpeeds" & c & ","
                            Next
                        Next
                        For x = 1 To 8
                            For c = 0 To 23
                                header = header & "S" & x & "HourlyFlows" & c & "," & "SRoadTypeFlows" & c & "," & "S" & x & "Charge" & c & "," & "S" & x & "LatentFlows" & c & "," & "S" & x & "NewHourlySpeeds" & c & ","
                            Next
                        Next
                    Case "ExtVar"
                        OutFileName = EVFilePrefix & "ExternalVariables.csv"
                        header = "Yeary,FlowID,PopZ1y,PopZ2y,GVAZ1y,GVAZ2y,M1Costy,MLanesy,DLanesy,SLanesy,MaxCapMy,MaxCapDy,MaxCapSy,M2Costy,M3Costy,M4Costy,M5Costy,M6Costy,D1Costy,D2Costy,D3Costy,D4Costy,D5Costy,D6Costy,S1Costy,S2Costy,S3Costy,S4Costy,S5Costy,S6Costy,S7Costy,S8Costy"
                    Case "RoadLinkNewCap"
                        OutFileName = FilePrefix & "RoadLinkNewCap.csv"
                        header = "FlowID,Yeary,RoadType,LanesAdded"
                End Select
            Case "RailZone"
                Select Case SubType
                    Case "Output"
                        OutFileName = FilePrefix & "RailZoneOutputData.csv"
                        TempFileName = FilePrefix & "RailZoneTemp.csv"
                        header = "Yeary,ZoneID,TripsStaty,Stationsy,Tripsy"
                        tempheader = "Yeary,ZoneID,PopZ,GvaZ,Cost,Stations,CarFuel,GJT,TripsStat,FareE"
                    Case "ExtVar"
                        OutFileName = EVFilePrefix & "RailZoneExtVar.csv"
                        header = "Yeary,ZoneID,PopZy,GvaZy,Costy,Stationsy,CarFuely,NewTripsy,GJTy,ElPy"
                End Select
            Case "RailLink"
                Select Case SubType
                    Case "Output"
                        OutFileName = FilePrefix & "RailLinkOutputData.csv"
                        TempFileName = FilePrefix & "RailLinkTemp.csv"
                        header = "Yeary,FlowID,Trainsy,Delaysy,CUy"
                        tempheader = "Yeary,FlowID,PopZ1,PopZ2,GVAZ1,GVAZ2,Delays,Cost,CarFuel,Trains,Tracks,MaxTDBase,CUOld,CUNew,BusyTrains,BusyPer,ModelPeakHeadway,CalculationCheck"
                    Case "ExtVar"
                        OutFileName = EVFilePrefix & "RailLinkExtVar.csv"
                        header = "Yeary,FlowID,Tracksy,PopZ1y,PopZ2y,GVAZ1y,GVAZ2y,Costy,CarFuely,MaxTDy,ElPy,ElTracksy,AddTrainsy"
                    Case "NewCap"
                        OutFileName = EVFilePrefix & "RailLinkNewCap.csv"
                        header = "FlowID,ChangeYear,TrackChange,MaxTDChange,TrainChange"
                    Case "RlLinkNewCap"
                        OutFileName = FilePrefix & "RailLinkNewCap.csv"
                        header = "Yeary,FlowID,TracksAdded"
                    Case "RlLinkFuelUsed"
                        OutFileName = FilePrefix & "RailLinkFuelConsumption.csv"
                        header = "Yeary,Diesely,Electricy"
                    Case "RlLinkElSchemes"
                        OutFileName = EVFilePrefix & "RailLinkElectrificationDates.csv"
                        header = "ElectricYear,FlowID,ElectricTracks,RouteKm"
                    Case "RlZoneElSchemes"
                        OutFileName = EVFilePrefix & "RailZoneElectrificationDates.csv"
                        header = "ElectricYear,ZoneID,ElectricStations"
                End Select
            Case "Seaport"
                Select Case SubType
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_SeaFreightOutputData"
                        OutFileName = FilePrefix & "SeaOutputData.csv"
                        'header = "Yeary, PortID, LiqBlky, DryBlky, GCargoy, LoLoy, RoRoy, GasOily, FuelOily"
                        header = "ModelRunID, StretegyID, Year, PortID, LiqBlk, DryBlk, GCargo, LoLo, RoRo, GasOil, FuelOil"
                    Case "Temp"
                        ToSQL = True
                        TableName = "TR_IO_SeaFreight"
                        OutFileName = FilePrefix & "SeaTemplate.csv"
                        'tempheader = "PortID, LiqBlk, DryBlk, GCargo, LoLo, RoRo, LBCap,DBCap,GCCap,LLCap,RRCap,GORPop,GORGva,Cost, AddedCap(1), AddedCap(2), AddedCap(3), AddedCap(4), AddedCap(5),"
                        header = "modelrun_id, year, PortID, LiqBlk, DryBlk, GCargo, LoLo, RoRo, AddedCap(1), AddedCap(2), AddedCap(3), AddedCap(4), AddedCap(5),"
                    Case "ExtVar"
                        ToSQL = True
                        TableName = "TR_O_SeaFreightExternalVariables"
                        OutFileName = EVFilePrefix & "SeaFreightExtVar.csv"
                        header = "ModelRunID,PortID,Yeary,LBCapy,DBCapy,GCCapy,LLCapy,RRCapy,GORPopy,GORGvay,Costy,FuelEffy"
                    Case "NewCap"
                        OutFileName = EVFilePrefix & "SeaFreightNewCap.csv"
                        header = "PortID,ChangeYear,NewLBCap,NewDBCap,NewGCCap,NewLLCap,NewRRCap"
                    Case "SeaNewCap"
                        OutFileName = FilePrefix & "SeaNewCap.csv"
                        header = "PortID,Yeary,LBCapAdded,DBCapAdded,GCCapAdded,LLCapAdded,RRCapAdded"
                End Select
            Case "AirNode"
                Select Case SubType
                    Case "Output"
                        OutFileName = FilePrefix & "AirNodeOutputData.csv"
                        TempFileName = FilePrefix & "AirNodeTemp.csv"
                        header = "Yeary,AirportID,AllPassy,DomPassy,IntPassy,ATMy,IntFuely"
                        tempheader = "AirportID,AllPassTotal,DomPass,IntPass,TermCapPPA,MaxATM,GORPop,GORGVA,Cost,PlaneSize,PlaneSizeInt,LFDom,LFInt,IntTripDist,AirportTripsLatent"
                    Case "ExtVar"
                        OutFileName = EVFilePrefix & "AirNodeExtVar.csv"
                        header = "Yeary,AirportID,GORPopy,GORGvay,Costy,TermCapy,MaxATMy,PlaneSizeDomy,PlaneSizeInty,LFDomy,LFInty,IntTripDist,FuelSeatKm"
                    Case "NewCap"
                        OutFileName = EVFilePrefix & "AirNodeNewCap.csv"
                        header = "NodeID,ChangeYear,NewTermCap,NewATMCap"
                    Case "AirNewCap"
                        OutFileName = FilePrefix & "AirNewCap.csv"
                        header = "AirportID,Yeary,TermCapAdded,RunCapAdded"
                End Select
            Case "AirFlow"
                Select Case SubType
                    Case "Output"
                        OutFileName = FilePrefix & "AirFlowOutputData.csv"
                        TempFileName = FilePrefix & "AirFlowTemp.csv"
                        header = "Yeary,FlowID,Tripsy,Fuely"
                        tempheader = "FlowID, OAirID, DAirID, Trips, PopOZ, PopDZ, GVAOZ, GVADZ, Cost, FlowKm, AirFlowTripsLatent, AirFlowCapConstant0, AirFlowCapConstant1"
                    Case "ExtVar"
                        OutFileName = EVFilePrefix & "AirFlowExtVar.csv"
                        header = "Yeary,FlowID,PopOZy,PopDZy,GVAOZy,GVADZy,Costy"
                End Select
            Case "Logfile"
                OutFileName = FilePrefix & "TransportCDAMLog.txt"
                header = "Transport CDAM Log File"
        End Select

        'Check if prefix has been set - if not then use default
        'Error may occur in readdata function, as temp file name could be different at different time
        'If FilePrefix = "" Then
        '    FilePrefix = System.DateTime.Now.Year & System.DateTime.Now.Month & System.DateTime.Now.Day & System.DateTime.Now.Hour & System.DateTime.Now.Minute & System.DateTime.Now.Second
        'End If


        If ToSQL = True Then
            'use headers for table field names
            aryFieldNames.Add(Split(header, ","))
            'use array data for field values
            For iy = 1 To UBound(OutputArray, 1)
                'exit if write to the end of the data
                If OutputArray(iy, 0) Is Nothing Then
                    Exit For
                End If
                For ix = 0 To UBound(OutputArray, 2)
                    aryFieldValues.Add(UnNull(OutputArray(iy, ix), VariantType.String))
                Next
                'Insert data into table
                SaveArrayToSQLTable(aryFieldNames, aryFieldValues, TableName, "", True)
            Next
        Else
            'write to local folder if not using database
            'If creating a new file then create headers
            'create the output file if new file is required or go to the last line if not
            If IsNewFile_IsInsert = True Then
                OutputFile = New FileStream(Connection & OutFileName, IO.FileMode.CreateNew, IO.FileAccess.ReadWrite)
                OutputWrite = New IO.StreamWriter(OutputFile, System.Text.Encoding.Default)
                'write header row 
                OutputWrite.WriteLine(header)
            ElseIf IsNewFile_IsInsert = False Then
                OutputFile = New FileStream(Connection & OutFileName, IO.FileMode.Open, IO.FileAccess.ReadWrite)
                OutputRead = New IO.StreamReader(OutputFile, System.Text.Encoding.Default)
                OutputWrite = New IO.StreamWriter(OutputFile, System.Text.Encoding.Default)
                OutputRead.ReadToEnd()
            End If

            'Some value are null at the the beginning years so this check may return false

            'check to make sure field count is the same as the header count
            'If fieldcount <> headcount Then
            '    MsgBox("Template fields do not match output data fields")
            '    Return False
            'End If


            'loop through array to generate lines in output file
            For iy = 1 To UBound(OutputArray, 1)
                'exit if write to the end of the data
                If OutputArray(iy, 0) Is Nothing Then
                    Exit For
                End If
                'Build a line to write
                Line = ""
                For ix = 0 To UBound(OutputArray, 2)
                    Line += UnNull(OutputArray(iy, ix), VariantType.String) & ","
                Next
                'Delete the last comma
                Line = Line.Substring(0, Len(Line) - 1)
                'Write the line to the output file

                OutputWrite.WriteLine(Line)
            Next

            OutputWrite.Close()

        End If



        Return True

    End Function

    Public Function SaveArrayToSQLTable(ByRef aryFieldNames As ArrayList, ByRef aryFieldValues As ArrayList, _
                                 ByVal TableName As String, ByVal IDField As String, _
                                 ByVal IsInsert As Boolean, Optional ByVal KeyID As Integer = 0) As Boolean

        Dim strSQL_N As String = ""
        Dim strSQL_V As String = ""
        Dim strSQL_NV As String = ""
        Dim strSQL_All As String = ""
        Dim i As Integer

        Try

            'Note that the field named FormKey is taken out as it is not a field name in the database
            'It's used to keep track of the value of the Field that is regarded as the FormKey
            If IsInsert = True Then
                'Get a list of Field Names
                For i = 0 To aryFieldNames.Count - 1
                    strSQL_N &= Chr(34) & aryFieldNames.Item(i) & Chr(34) & ", "
                Next
                'Get rid of the last comma and space
                strSQL_N = Left(strSQL_N, Len(strSQL_N) - 2)

                'Get a list of Field Values
                For i = 0 To aryFieldNames.Count - 1
                    strSQL_V &= aryFieldValues.Item(i) & ", "
                Next
                'Get rid of the last comma and space
                strSQL_V = Left(strSQL_V, Len(strSQL_V) - 2)

                strSQL_All = "INSERT INTO " & TableName & " (" & strSQL_N & ") "
                strSQL_All = strSQL_All & "VALUES (" & strSQL_V & ")"
            Else
                strSQL_NV = ""
                'Get a list of Field Names = Values
                For i = 0 To aryFieldNames.Count - 1
                    strSQL_NV &= aryFieldNames.Item(i) & " = " & aryFieldValues.Item(i) & ", "
                Next
                'Get rid of the last comma and space
                strSQL_NV = Left(strSQL_NV, Len(strSQL_NV) - 2)

                strSQL_All = "UPDATE " & TableName & " SET " & strSQL_NV
                strSQL_All &= " WHERE " & IDField & " = " & KeyID
            End If

            ConnectToDBase()
            cmd.Connection = m_conn
            cmd.CommandText = strSQL_All

            Dim da As Odbc.OdbcDataAdapter = New Odbc.OdbcDataAdapter(cmd)
            Dim ds As New DataSet
            da.Fill(ds, "Data")

            Return True
        Catch ex As Exception
            Throw ex
            Return False
        End Try
    End Function

    Public Function UnNull(ByVal vntData As Object, ByVal datatype As VariantType) As Object
        'default
        UnNull = vntData

        If IsDBNull(vntData) Or IsNothing(vntData) Then
            Select Case datatype
                Case vbString
                    UnNull = ""
                Case vbDate
                    UnNull = "1/1/1900"
                Case vbSingle, vbInteger, vbLong, vbByte, vbCurrency, vbDecimal, vbDouble
                    UnNull = 0
                Case vbBoolean
                    UnNull = False
                Case Else
                    UnNull = ""
            End Select

        End If
    End Function

End Module
