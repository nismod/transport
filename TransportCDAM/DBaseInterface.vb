Option Explicit On
Imports System.IO

Module DBaseInterface
    Public RunRoadLink As Boolean
    Public RunRoadZone As Boolean
    Public RunRailLink As Boolean
    Public RunRailZone As Boolean
    Public RunAir As Boolean
    Public RunSea As Boolean
    Public DirPath As String
    Public LogFile As IO.FileStream
    Public lf As IO.StreamWriter
    Public LogLine As String
    Public FilePrefix As String
    Public CreateExtVars As Boolean
    Public UpdateExtVars As Boolean
    Public NewRdLEV As Boolean
    Public NewRdZEV As Boolean
    Public NewRlLEV As Boolean
    Public NewRlZEV As Boolean
    Public NewAirEV As Boolean
    Public NewSeaEV As Boolean
    Public EVFilePrefix As String
    Public RunModel As Boolean = True
    Public NewRdLCap, NewRdZCap, NewRlLCap, NewRlZCap, NewAirCap, NewSeaCap As Boolean
    Public CapFilePrefix As String
    Public EVFileSuffix As String
    Public RdZEVSource, RlLEVSource, RlZEVSource, AirEVSource, SeaEVSource As String
    Public RdLPopSource, RdZPopSource, RlLPopSource, RlZPopSource, AirPopSource, SeaPopSource, RdLEcoSource, RdZEcoSource, RlLEcoSource, RlZEcoSource, AirEcoSource, SeaEcoSource, RdLEneSource, RdZEneSource, RlLEneSource, RlZEneSource, AirEneSource, SeaEneSource, RlLOthSource, RlZOthSource As String
    Public DBasePop, DBaseEco, DBaseEne, DBasePopG, DBaseCheck As Boolean
    Public DBasePopFile, DBaseEcoFile, DBaseEneFile, DBasePopGFile As String
    Public ZonePopFile, ZoneEcoFile, ZoneEneFile As IO.FileStream
    Public zpr, zer, znr As IO.StreamReader
    Public PopLookup, EcoLookup As New Dictionary(Of Long, Double)
    Public PopYearLookup, EcoYearLookup As New Dictionary(Of String, Double)
    Public ControlFile, SubStrategyFile As IO.FileStream
    Public BuildInfra As Boolean
    Public CUCritValue As Double
    Public VariableEl As Boolean
    Public ElCritValue As Double
    Public CongestionCharge, CarbonCharge, WPPL, RailCCharge, AirCCharge, RlCaCharge, AirCaCharge, UpdateInput As Boolean
    Public ConChargePer, RailChargePer, AirChargePer As Double
    Public ConChargeYear, WPPLYear, RlCChargeYear, CarbChargeYear, RlCaChYear, AirChargeYear, AirCaChYear As Long
    Public WPPLPer As Double
    Public RlElect As Boolean
    Public ElectKmPerYear As Double
    Public SWDisagg As Boolean
    Public SmarterChoices, SmartFrt, UrbanFrt As Boolean
    Public SmartIntro, SmartYears, SmFrtIntro, SmFrtYears, UrbFrtIntro, UrbFrtYears As Long
    Public SmartPer, SmFrtPer, UrbFrtPer As Double
    Public NewSeaTonnes, NewRoadLanes, NewRailTracks, NewAirRun, NewAirTerm As Double
    Public RailCUPeriod As String
    Public RlPeakHeadway As Double
    Public RdZSpdSource As String
    Public RoadCapNum As Long
    Public RLCapYear(1) As Long
    Public TripRates As String
    Public StartYear As Integer
    Public Duration As Integer
    Public logNum As Integer
    Public logarray(47, 0) As String
    Public ScenarioID As Integer
    Public Scenario As Integer
    Public StrategyID As Integer
    Public StrategyCode As String
    Public SubStrategy As Integer
    Public DBaseMode As Boolean = True
    Public theYear As Integer

    Public OZone, DZone As Long
    Dim ModelType As String
    Dim Subtype As String

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
    Dim seaDemogArray(,) As String = Nothing
    Dim airDemogArray(,) As String = Nothing
    Dim zoneDemogArray(,) As String = Nothing
    Dim seaGVAArray(,) As String = Nothing
    Dim airGVAArray(,) As String = Nothing
    Dim zoneGVAArray(,) As String = Nothing
    Dim dataArray(,) As String = Nothing

    'Global variables
    Public g_modelRunID As Integer
    Public g_modelRunYear As Integer
    Public g_initialYear As Integer = 2010


    Sub ConnectToDBase()
        Try
            'If there is no connection to the database then establish one
            If m_conn Is Nothing Then
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

        'TODO - This doesn't seem to be getting data from the dbase?

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

    'These Get Population/GVA functions return large amount of data (every zone/flow/port data for the year), which are not necessary. We can either call these function at the start of each year or improve these function to read specified data (Xucheng)
#Region "...Get Population..."

    Function get_population_data_by_airportID(ByVal year As Integer, ByVal PortID As Integer)
        Dim theSQL As String = ""

        If PortID = 1 Then
            'reset airGVAArray value to read from database
            airDemogArray = Nothing
        End If


        'If the Demographic data has not been loaded then load it for each zone or port.
        If airDemogArray Is Nothing Then

            theSQL = "SELECT * FROM cdam_get_population_data_by_model_run_id_per_tr_gor(" & g_modelRunID & "," & year & ",'air',9999) "
            theSQL &= " AS (scenario_id varchar, year integer, gender varchar, category varchar, value double precision, " & Chr(34) & "GORName" & Chr(34)
            theSQL &= " varchar, gor_id integer, tr_cdam_gor_id integer, "
            theSQL &= Chr(34) & "PortName" & Chr(34) & " varchar, port_id integer);"

            If LoadSQLDataToArray(airDemogArray, theSQL) = False Then
                airDemogArray = Nothing
                Return 0
            End If
        End If

        'Get the population for the specified airport
        For i = 1 To UBound(airDemogArray, 1)
            If CInt(airDemogArray(i, 9)) = PortID Then
                Return CDbl((airDemogArray(i, 4)) / 1000)
            End If
        Next

        'If portid not found then return 0
        Return 0

    End Function

    Function get_population_data_by_zoneID(ByVal year As Integer, ByVal ZoneID As Integer, ByVal zoneType As String, ByVal type As String, Optional ByVal ODZoneID As Integer = 0)
        Dim theSQL As String = ""

        '    If ZoneID = 1 Then
        'reset zoneDemogArray value at the beginning of each year
        '"OZ" has to be called first to avoid errors
        'If zoneType = "OZ" Or zoneType = "Zone" Then
        'zoneDemogArray = Nothing
        'End If
        'End If

        'If zoneDemogArray Is Nothing Then

        'call different SQL function for zone and for link
        If zoneType = "Zone" Then
            theSQL = "SELECT * FROM cdam_get_population_data_by_model_run_id_per_tr_zone(" & g_modelRunID & "," & year & "," & ZoneID & ") "
            '200115 edit - SQL commented out for next three lines and "9999" changed to "1" in previous line
            'theSQL &= " AS (scenario_id varchar, year integer, gender varchar, category varchar, " & Chr(34) & "DistrictName" & Chr(34)
            'theSQL &= " varchar, zone_id integer, " & Chr(34) & "ZoneName" & Chr(34) & " varchar, district_code varchar, "
            'theSQL &= "value double precision);"
        Else
            theSQL = "SELECT * FROM cdam_get_population_data_by_model_run_id_per_tr_flow(" & g_modelRunID & "," & year & "," & type & "," & ZoneID & ") "
            theSQL &= " AS (scenario_id varchar, year integer, gender varchar, category varchar, flow_id integer," & Chr(34) & "PopOZ" & Chr(34)
            theSQL &= " double precision, ozone_id integer, " & Chr(34) & "PopDZ" & Chr(34) & " double precision, "
            theSQL &= "dzone_id integer);"
        End If

        If LoadSQLDataToArray(zoneDemogArray, theSQL) = False Then
            zoneDemogArray = Nothing
            Return 0
        End If
        'End If

        'Get the population for the specified zone
        Select Case zoneType
            Case "OZ"
                For i = 1 To UBound(zoneDemogArray, 1)
                    If CInt(zoneDemogArray(i, 6)) = ODZoneID Then
                        Return (CDbl(zoneDemogArray(i, 5)) / 1000)
                    End If
                Next
            Case "DZ"
                For i = 1 To UBound(zoneDemogArray, 1)
                    If CInt(zoneDemogArray(i, 8)) = ODZoneID Then
                        Return (CDbl(zoneDemogArray(i, 7)) / 1000)
                    End If
                Next
            Case "Zone"
                For i = 1 To UBound(zoneDemogArray, 1)
                    If CInt(zoneDemogArray(i, 5)) = ZoneID Then
                        Return (CDbl(zoneDemogArray(i, 8)) / 1000)
                    End If
                Next
            Case Else
                Return 0
        End Select


        'If portid not found then return 0
        Return 0

    End Function


    Function get_population_data_by_seaportID(ByVal year As Integer, ByVal PortID As Integer) As Double
        Dim theSQL As String = ""

        If PortID = 1 Then
            'reset seaGVAArray value to read from database
            seaDemogArray = Nothing
        End If


        'If the Demographic data has not been loaded then load it for each zone or port.
        If seaDemogArray Is Nothing Then

            theSQL = "SELECT * FROM cdam_get_population_data_by_model_run_id_per_tr_gor(" & g_modelRunID & "," & year & ",'sea',9999) "
            theSQL &= " AS (scenario_id varchar, year integer, gender varchar, category varchar, value double precision, " & Chr(34) & "GORName" & Chr(34)
            theSQL &= " varchar, gor_id integer, tr_cdam_gor_id integer, "
            theSQL &= Chr(34) & "PortName" & Chr(34) & " varchar, port_id integer);"

            If LoadSQLDataToArray(seaDemogArray, theSQL) = False Then
                seaDemogArray = Nothing
                Return 0
            End If
        End If

        'Get the population for the specified port
        For i = 1 To UBound(seaDemogArray, 1)
            If CInt(seaDemogArray(i, 9)) = PortID Then
                Return CDbl((seaDemogArray(i, 4)) / 1000)
            End If
        Next

        'If portid not found then return 0
        Return 0

    End Function

#End Region

#Region "...Get GVA..."

    Function get_gva_data_by_zoneID(ByVal year As Integer, ByVal ZoneID As Integer, ByVal zoneType As String, ByVal type As String, Optional ByVal ODZoneID As Integer = 0)
        Dim theSQL As String = ""

        'call different SQL function for zone and for link
        If zoneType = "Zone" Then
            theSQL = "SELECT * FROM cdam_get_economics_data_by_model_run_id_per_tr_zone(" & g_modelRunID & "," & year & "," & ZoneID & ") "
            '200115 change next two lines commented out and "9999" changed to "1" in previous line
            'theSQL &= " AS (economics_scenario_id varchar, year integer, " & Chr(34) & "GOR" & Chr(34)
            'theSQL &= " varchar, zone_id integer, " & Chr(34) & "GVAZ" & Chr(34) & " double precision);"
        Else
            theSQL = "SELECT * FROM cdam_get_economics_data_by_model_run_id_per_tr_flow(" & g_modelRunID & "," & year & "," & type & "," & ZoneID & ") "
            theSQL &= " AS (economics_scenario_id varchar, year integer, flow_id integer," & Chr(34) & "GVAOZ" & Chr(34)
            theSQL &= " double precision, ozone_id integer, " & Chr(34) & "GVADZ" & Chr(34) & " double precision, "
            theSQL &= "dzone_id integer);"
        End If


        If LoadSQLDataToArray(zoneGVAArray, theSQL) = False Then
            zoneGVAArray = Nothing
            Return 0
        End If
        'End If

        'Get the gva for the specified zone
        Select Case zoneType
            Case "OZ"
                For i = 1 To UBound(zoneGVAArray, 1)
                    If CInt(zoneGVAArray(i, 4)) = ODZoneID Then
                        Return (CDbl(zoneGVAArray(i, 3)) / 1000000)
                    End If
                Next
            Case "DZ"
                For i = 1 To UBound(zoneGVAArray, 1)
                    If CInt(zoneGVAArray(i, 6)) = ODZoneID Then
                        Return (CDbl(zoneGVAArray(i, 5)) / 1000000)
                    End If
                Next
            Case "Zone"
                For i = 1 To UBound(zoneGVAArray, 1)
                    If CInt(zoneGVAArray(i, 3)) = ZoneID Then
                        Return (CDbl(zoneGVAArray(i, 4)) / 1000000)
                    End If
                Next
            Case Else
                Return 0
        End Select

        'If portid not found then return 0
        Return 0

    End Function

    Function get_gva_data_by_airportID(ByVal year As Integer, ByVal PortID As Integer)
        Dim theSQL As String = ""
        Dim i As Integer

        If PortID = 1 Then
            'reset airGVAArray value to read from database
            airGVAArray = Nothing
        End If

        If airGVAArray Is Nothing Then

            theSQL = "SELECT * FROM cdam_get_economics_data_by_model_run_id_per_tr_gor(" & g_modelRunID & "," & year & ",'air', 9999) "
            theSQL &= " AS (scenario_id varchar, year integer," & Chr(34) & "GORName" & Chr(34)
            theSQL &= " varchar, " & Chr(34) & "GVAZ" & Chr(34) & " double precision, tr_cdam_gor_id integer, "
            theSQL &= Chr(34) & "PortName" & Chr(34) & " varchar, port_id integer);"

            If LoadSQLDataToArray(airGVAArray, theSQL) = False Then
                airGVAArray = Nothing
                Return 0
            End If
        End If

        'Get the population for the specified zone
        For i = 1 To UBound(airGVAArray, 1)
            If CInt(airGVAArray(i, 6)) = PortID Then
                Return CDbl((airGVAArray(i, 3)) / 1000000)
            End If
        Next

        'If portid not found then return 0
        Return 0

    End Function

    Function get_gva_data_by_seaportID(ByVal year As Integer, ByVal PortID As Integer)
        Dim theSQL As String = ""
        Dim i As Integer

        If PortID = 1 Then
            'reset seaGVAArray value to read from database
            seaGVAArray = Nothing
        End If

        If seaGVAArray Is Nothing Then

            theSQL = "SELECT * FROM cdam_get_economics_data_by_model_run_id_per_tr_gor(" & g_modelRunID & "," & year & ",'sea', 9999) "
            theSQL &= " AS (scenario_id varchar, year integer," & Chr(34) & "GORName" & Chr(34)
            theSQL &= " varchar, " & Chr(34) & "GVAZ" & Chr(34) & " double precision, tr_cdam_gor_id integer, "
            theSQL &= Chr(34) & "PortName" & Chr(34) & " varchar, port_id integer);"

            If LoadSQLDataToArray(seaGVAArray, theSQL) = False Then
                seaGVAArray = Nothing
                Return 0
            End If
        End If

        'Get the population for the specified zone
        For i = 1 To UBound(seaGVAArray, 1)
            If CInt(seaGVAArray(i, 6)) = PortID Then
                Return CDbl((seaGVAArray(i, 3)) / 1000000)
            End If
        Next

        'If portid not found then return 0
        Return 0

    End Function


#End Region
    Sub get_zone_by_flowid(ByVal FlowID As Integer, ByRef Zone1ID As Integer, ByRef Zone2ID As Integer, ByVal type As String)
        Dim theSQL As String = ""
        Dim InputArray(,) As String = Nothing

        'get origin zone ID and destination zone ID based on the type and flow ID
        Select Case type
            Case "air"
                theSQL = "SELECT id, ozone_id, dzone_id FROM " & Chr(34) & "TR_LU_AirFlows" & Chr(34) & " WHERE id = " & FlowID
            Case "rail"
                theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailLinkFlows" & Chr(34) & " WHERE id = " & FlowID
            Case "road"
                theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RoadInputFlows" & Chr(34) & " WHERE id = " & FlowID
        End Select

        If LoadSQLDataToArray(InputArray, theSQL) = True Then
            Zone1ID = InputArray(1, 1)
            Zone2ID = InputArray(1, 2)
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
    '               Year - current year of the calculation, this is for external variable file to read from the correct line
    '               whereID - an integer to be used in the WHERE clause of the SQL
    '               Connection - file path - to be replaced with database connection string
    '****************************************************************************************

    Function ReadData(ByVal Type As String, ByVal SubType As String, ByRef InputArray(,) As String,
                       Optional ByVal Year As Integer = 0, Optional whereID As Integer = 0,
                    Optional Connection As String = "") As Boolean
        Dim TheFileName As String = ""
        Dim DataFile As FileStream
        Dim DataRead As StreamReader
        Dim dbheadings As String
        Dim dbline As String
        Dim dbarray() As String
        Dim iR As Integer = 0, iC As Integer = 0
        Dim DataRows As Integer = 0, DataColumns As Integer = 0
        Dim theSQL As String = ""

        'Get connection path if not in Database mode
        If DBaseMode = False Then
            'Check if file path has been selected - if not then use default.
            If Connection = "" Then Connection = DirPath
            If Connection.Substring(Len(Connection) - 1, 1) <> "\" Then
                'Make sure the file path ends with at \
                Connection = Connection & "\"
            End If
        End If

        'Get the filename of datafile based on Type and SubType
        'Get the initial input data file if it is year 1, otherwise get the temp file
        'the size of the array must be correct
        Select Case Type
            Case "System"
                Select Case SubType
                    Case "ModelRunDetails"
                        theSQL = "SELECT * FROM " & Chr(34) & "ISL_ModelRuns" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID
                    Case "Parameters"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_I_Parameters_Run" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID
                    Case Else
                        'for error handling
                End Select
            Case "Inputs"
                Select Case SubType
                    Case "Parameters"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_I_Parameters_Run" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID

                End Select

            Case "RoadZone"
                Select Case SubType
                    Case "Input"
                        If Year = g_initialYear Then
                            TheFileName = "RoadZoneInputData2010.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RoadZone_InitialData" & Chr(34) & " ORDER BY zone_id AND modelrun_id = " & g_modelRunID & " AND year = " & Year
                        Else
                            TheFileName = FilePrefix & "RoadZoneTemp.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadZone" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & " and year = " & Year - 1
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "RoadZoneExtVar" & EVFileSuffix & ".csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadZoneExternalVariables" & Chr(34) & " WHERE year = " & Year & " ORDER BY zone_id AND modelrun_id = " & g_modelRunID
                    Case "NewCap"
                        TheFileName = CapFilePrefix & "RoadZoneCapChange.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RoadZoneCapacityChange" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "CapChange"
                        TheFileName = FilePrefix & "RoadZoneCapChange.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadZoneCapacityChange" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\RoadZoneElasticities.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_I_RoadZoneElasticities_Run" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case Else
                        'for error handling
                End Select
            Case "RoadLink"
                Select Case SubType
                    Case "Input"
                        If Year = g_initialYear Then
                            TheFileName = "RoadInputData2010.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RoadLink_InitialData" & Chr(34) & " WHERE year = " & Year & " AND modelrun_id=" & g_modelRunID & " ORDER BY year, flow_id"
                        Else
                            TheFileName = FilePrefix & "RoadLinkTemp.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RailLink" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year - 1
                        End If
                    Case "Temp Annual"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadLink_Annual" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year - 1
                    Case "Temp Hourly"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadLink_Hourly" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year - 1
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "ExternalVariables" & EVFileSuffix & ".csv"
                        If whereID = 0 Then
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadLinkExternalVariables" & Chr(34) & " WHERE year = " & Year & " AND modelrun_id=" & g_modelRunID & " ORDER BY flow_id"
                        Else
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RoadLinkExternalVariables" & Chr(34) & " WHERE year = " & Year & " AND modelrun_id=" & g_modelRunID & " AND flow_id = " & whereID & " ORDER BY flow_id"
                        End If
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "RoadLinkCapChange.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RoadLinkCapacityChange" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "NewCap"
                        TheFileName = EVFilePrefix & "RoadLinkNewCap.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_O_RoadLinkNewCapacity" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\RoadLinkElasticities.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_I_RoadLinkElasticities_Run" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "FreeFlowSpeeds"
                        'A header has been added to the original file to keep in the same format
                        TheFileName = "FreeFlowSpeedsv0.7.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RoadLink_FreeFlowSpeeds" & Chr(34)
                    Case "DailyProfile"
                        'A header has been added to the original file to keep in the same format
                        TheFileName = "DailyTripProfile.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RoadLink_DailyProfile" & Chr(34)
                    Case Else
                        'for error handling
                End Select
            Case "RailZone"
                Select Case SubType
                    Case "Input"
                        If Year = g_initialYear Then
                            TheFileName = "RailZoneInputData2010.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailZone_InitialData" & Chr(34) & " WHERE year = " & Year & " ORDER BY zone_id"
                        Else
                            TheFileName = FilePrefix & "RailZoneTemp.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RailZone" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year - 1
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "RailZoneExtVar" & EVFileSuffix & ".csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RailZoneExternalVariables" & Chr(34) & " WHERE year = " & Year & " AND modelrun_id=" & g_modelRunID & " ORDER BY zone_id"
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "RailZoneCapChange.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailZoneCapacityChange" & Chr(34) & " WHERE  changeyear = " & Year
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\RailZoneElasticities.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_I_RailZoneElasticities_Run" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "ElSchemes"
                        TheFileName = EVFilePrefix & "RailZoneElectrificationDates.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RailZoneElectrificationDates" & Chr(34) & " WHERE modelrun_id=" & g_modelRunID
                    Case "EVScale"
                        TheFileName = "RailZoneEVScaling.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailZone_EVScaling" & Chr(34) & " WHERE year = " & Year
                    Case Else
                        'for error handling
                End Select
            Case "RailLink"
                Select Case SubType
                    Case "Input"
                        If Year = g_initialYear Then
                            TheFileName = "RailLinkInputData2010.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailLink_InitialData" & Chr(34) & " WHERE year = " & Year & " ORDER BY year, flow_id"
                        Else
                            TheFileName = FilePrefix & "RailLinkTemp.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RailLink" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year - 1
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "RailLinkExtVar" & EVFileSuffix & ".csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RailLinkExternalVariables" & Chr(34) & " WHERE year = " & Year & " AND modelrun_id=" & g_modelRunID & " ORDER BY flow_id"
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "RailLinkCapChange.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailLinkCapacityChange" & Chr(34) & " WHERE changeyear = " & Year
                    Case "NewCap"
                        TheFileName = EVFilePrefix & "RailLinkNewCap.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_O_RailLinkNewCapacity" & Chr(34) & " WHERE changeyear = " & Year
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\RailLinkElasticities.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_I_RailLinkElasticities_Run" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "ElSchemes"
                        TheFileName = EVFilePrefix & "RailLinkElectrificationDates.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_RailLinkElectrificationDates" & Chr(34) & " WHERE modelrun_id=" & g_modelRunID & "and year = " & Year
                    Case "EVScale"
                        TheFileName = "RailLinkEVScaling.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailLink_EVScaling" & Chr(34) & " WHERE year = " & Year
                    Case "OldRlEl"
                        TheFileName = "RailElectrificationSchemes.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailLink_ElectrificationSchemes"
                    Case "OldRzEl"
                        TheFileName = "RailZoneElectrificationSchemes.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_RailZone_ElectrificationSchemes"
                    Case Else
                        'for error handling
                End Select
            Case "Seaport"
                Select Case SubType
                    Case "Input"
                        If Year = g_initialYear Then
                            TheFileName = "SeaFreightInputData.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_SeaFreight_InitialData" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & " AND year = " & Year & " ORDER BY port_id"
                        Else
                            TheFileName = FilePrefix & "SeaTemplate.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_SeaFreight" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year - 1
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "SeaFreightExtVar" & EVFileSuffix & ".csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_SeaFreightExternalVariables" & Chr(34) & " WHERE year = " & Year & " AND modelrun_id=" & g_modelRunID
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "SeaFreightCapChange.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_SeaFreightCapacityChange" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "NewCap"
                        TheFileName = EVFilePrefix & "SeaFreightNewCap.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_O_SeaFreightNewCapacity" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\SeaFreightElasticities.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_I_SeaFreightElasticities_Run" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case Else
                        'for error handling
                End Select
            Case "AirNode"
                Select Case SubType
                    Case "Input"
                        If Year = g_initialYear Then
                            TheFileName = "AirNodeInputData2010.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_AirNode_InitialData" & Chr(34) & " WHERE year = " & 2010 & " AND modelrun_id=" & g_modelRunID & " ORDER BY year, airport_id"
                        Else
                            TheFileName = FilePrefix & "AirNodeTemp.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_AirNode" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year - 1
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "AirNodeExtVar" & EVFileSuffix & ".csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_AirNodeExternalVariables" & Chr(34) & " AND modelrun_id=" & g_modelRunID & " WHERE year = " & Year
                    Case "CapChange"
                        TheFileName = CapFilePrefix & "AirNodeCapChange.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_AirNodeCapacityChange" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "NewCap"
                        TheFileName = EVFilePrefix & "AirNodeNewCap.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_O_AirNodeNewCapacity" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case "Elasticity"
                        TheFileName = "Elasticity Files\TR" & SubStrategy & "\AirElasticities.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_I_AirElasticities_Run" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year
                    Case Else
                        'for error handling
                End Select
            Case "AirFlow"
                Select Case SubType
                    Case "Input"
                        If Year = g_initialYear Then
                            TheFileName = "AirFlowInputData2010.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_LU_AirFlow_InitialData" & Chr(34) & " WHERE year = " & 2010 & " ORDER BY year, flow_id"
                        Else
                            TheFileName = FilePrefix & "AirFlowTemp.csv"
                            theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_AirFlow" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & "and year = " & Year - 1
                        End If
                    Case "ExtVar"
                        TheFileName = EVFilePrefix & "AirFlowExtVar.csv"
                        theSQL = "SELECT * FROM " & Chr(34) & "TR_IO_AirFlowExternalVariables" & Chr(34) & " AND modelrun_id=" & g_modelRunID & " WHERE year = " & Year
                    Case Else
                        'for error handling
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

            If g_modelRunYear = g_initialYear Then
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
            ReDim aryData(DataRows + 1, DataColumns - 1)
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
            Stop
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
        Dim OutputFile As IO.FileStream
        Dim OutputWrite As StreamWriter
        Dim OutputRead As StreamReader
        Dim Line As String = ""
        Dim ix As Integer, iy As Integer
        Dim header As String
        Dim aryFieldNames As New ArrayList
        Dim aryFieldValues As New ArrayList
        Dim ToSQL As Boolean = False
        Dim TempTableName As String = ""
        Dim TableName As String = ""

        'Get connection path if not in Database mode
        If DBaseMode = False Then
            'Check if file path has been selected - if not then use default.
            If Connection = "" Then Connection = DirPath
            If Connection.Substring(Len(Connection) - 1, 1) <> "\" Then
                'Make sure the file path ends with at \
                Connection = Connection & "\"
            End If
        End If
        'Get the filename of datafile based on Type and SubType
        'TODO - replace with database calls
        Select Case Type
            Case "AirFlow"
                Select Case SubType
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_AirFlowOutputData"
                        OutFileName = FilePrefix & "AirFlowOutputData.csv"
                        header = "modelrun_id, flow_id, year, trips, fuel"
                    Case "Temp"
                        ToSQL = True
                        TableName = "TR_IO_AirFlow"
                        OutFileName = FilePrefix & "AirFlowTemp.csv"
                        header = "modelrun_id, year, flow_id, trips, air_flow_trips_latent, air_flow_cap_constant0, air_flow_cap_constant1, flow_km"
                    Case "ExtVar"
                        ToSQL = True
                        TableName = "TR_IO_AirFlowExternalVariables"
                        OutFileName = EVFilePrefix & "AirFlowExtVar.csv"
                        header = "modelrun_id, year, flow_id, ozone_pop, dzone_pop, ozone_gva, cost"
                End Select
            Case "AirNode"
                Select Case SubType
                    Case "NewCap"
                        ToSQL = True
                        TableName = "TR_O_AirNodeNewCapacity"
                        OutFileName = EVFilePrefix & "AirNodeNewCap.csv"
                        header = "modelrun_id, airport_id, changeyear, new_term_capacity, new_atm_cap"
                    Case "NewCap_Add"
                        TableName = "TR_O_AirNodeNewCapacity_Add"
                        OutFileName = FilePrefix & "AirNewCap.csv"
                        header = "modelrun_id, airport_id, changeyear, term_cap_added, run_cap_added"
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_AirNodeOutputData"
                        OutFileName = FilePrefix & "AirNodeOutputData.csv"
                        header = "modelrun_id, airport_id, year, all_pass, dom_pass, int_pass, atm, int_fuel"
                    Case "Temp"
                        ToSQL = True
                        TableName = "TR_IO_AirNode"
                        OutFileName = FilePrefix & "AirNodeTemp.csv"
                        header = "modelrun_id, year, airport_id, all_pass_total, dom_pass, int_pass, airport_trips_latent"
                    Case "ExtVar"
                        ToSQL = True
                        TableName = "TR_IO_AirNodeExternalVariables"
                        OutFileName = EVFilePrefix & "AirNodeExtVar.csv"
                        header = "modelrun_id, airport_id, year, gor_pop, gor_gva, cost, term_cap, max_atm, plane_size_dom, lf_dom, lf_int, int_trip_dist, fuel_seat_km, plane_size_int"
                End Select
            Case "RailLink"
                Select Case SubType
                    Case "ElSchemes"
                        ToSQL = True
                        TableName = "TR_O_RailLinkElectrificationDates"
                        OutFileName = EVFilePrefix & "RailLinkElectrificationDates.csv"
                        header = "modelrun_id, flow_id, electric_year, electric_tracks, route_km"
                    Case "FuelUsed"
                        ToSQL = True
                        TableName = "TR_O_RailLinkFuelConsumption"
                        OutFileName = FilePrefix & "RailLinkFuelConsumption.csv"
                        header = "modelrun_id, year, diesel, electric"
                    Case "NewCap"
                        ToSQL = True
                        TableName = "TR_O_RailLinkNewCapacity"
                        OutFileName = EVFilePrefix & "RailLinkNewCap.csv"
                        header = "modelrun_id, flow_id, changeyear, track_change, max_td_change, train_change"
                    Case "NewCap_Added"
                        TableName = "TR_O_RailLinkNewCapacity_Add"
                        OutFileName = FilePrefix & "RailLinkNewCapacity.csv"
                        header = "modelrun_id, flow_id, changeyear, tracks_added"
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_RailLinkOutputData"
                        OutFileName = FilePrefix & "RailLinkOutputData.csv"
                        header = "modelrun_id, flow_id, year, trains, delays, cu"
                    Case "Temp"
                        ToSQL = True
                        TableName = "TR_IO_RailLink"
                        OutFileName = FilePrefix & "RailLinkTemp.csv"
                        header = "modelrun_id, year, flow_id, delays, cost, trains, tracks, max_td_base, cu_old, cu_new, busy_trains, busy_per, model_peak_headway, calculation_check"
                    Case "ExtVar"
                        ToSQL = True
                        TableName = "TR_IO_RailLinkExternalVariables"
                        OutFileName = EVFilePrefix & "RailLinkExtVar.csv"
                        header = "modelrun_id, flow_id, year, tracks, pop_z1, pop_z2, gva_z1, gva_z2, cost, car_fuel, max_td, el_p, el_tracks, add_trains"
                End Select
            Case "RailZone"
                Select Case SubType
                    Case "ElSchemes"
                        ToSQL = True
                        TableName = "TR_O_RailZoneElectrificationDates"
                        OutFileName = EVFilePrefix & "RailZoneElectrificationDates.csv"
                        header = "modelrun_id, zone_id, electric_year, electric_stations"
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_RailZoneOutputData"
                        OutFileName = FilePrefix & "RailZoneOutputData.csv"
                        header = "modelrun_id, year, zone_id, trips_stat, stations, trips"
                    Case "Temp"
                        ToSQL = True
                        TableName = "TR_IO_RailZone"
                        OutFileName = FilePrefix & "RailZoneTemp.csv"
                        header = "modelrun_id, year, zone_id, trips_stat, fare_e"
                    Case "ExtVar"
                        ToSQL = True
                        TableName = "TR_IO_RailZoneExternalVariables"
                        OutFileName = EVFilePrefix & "RailZoneExtVar.csv"
                        header = "modelrun_id, zone_id, year, pop_z, gva_z, cost, stations, car_fuel, new_trips, gjt, elp"
                End Select
            Case "RoadLink"
                Select Case SubType
                    Case "NewCap"
                        ToSQL = True
                        TableName = "TR_O_RoadLinkNewCapacity"
                        OutFileName = EVFilePrefix & "RoadLinkNewCap.csv"
                        header = "modelrun_id, flow_id, changeyear, mlane_change, dlane_change, slane_change"
                    Case "NewCap_Add"
                        TableName = "TR_O_RoadLinkNewCapacity_Added"
                        OutFileName = FilePrefix & "RoadLinkNewCap.csv"
                        header = "modelrun_id, flow_id, changeyear, road_type, lanes_added"
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_RoadOutputFlows"
                        header = "modelrun_id, flow_id, year, pcu_total, speed_mean, pcu_mway, pcu_dual, pcu_sing, spd_mway, spd_dual, spd_sing, msc1, msc2, msc3, msc4, msc5, msc6, dsc1, dsc2, dsc3, dsc4, dsc5, dsc6, ssc1, ssc2, ssc3, ssc4, ssc5, ssc6, ssc7, ssc8, msc1_spd, msc2_spd, msc3_spd, msc4_spd, msc5_spd, msc6_spd, dsc1_spd, dsc2_spd, dsc3_spd, dsc4_spd, dsc5_spd, dsc6_spd, ssc1_spd, ssc2_spd, ssc3_spd, ssc4_spd, ssc5_spd, ssc6_spd, ssc7_spd, ssc8_spd, mway_latent, dual_latent, sing_latent, mfull_hrs, dfull_hrs, sfull_hrs, mway_cost, dual_cost, sing_cost"
                    Case "ExtVar"
                        ToSQL = True
                        TableName = "TR_IO_RoadLinkExternalVariables"
                        OutFileName = EVFilePrefix & "ExternalVariables.csv"
                        header = "modelrun_id, flow_id, year, , pop_z1, pop_z2, gva_z1, gva_z2, m_lanes, d_lanes, s_lanes, m_max_cap_m, d_max_cap_m, s_max_cap_m, m1_cost, m2_cost, m3_cost, m4_cost, m5_cost, m6_cost, d1_cost, "
                        header = header & " d2_cost, d3_cost, d4_cost, d5_cost, d6_cost, s1_cost, s2_cost, s3_cost, s4_cost, s5_cost, s6_cost, s7_cost, s8_cost, sc0_p_cost, sc0_be_cost, sc0_hfc_cost, sc1_p_cost, sc1_d_costs, "
                        header = header & " sc1_ph_cost, sc1_dh_cost, sc1_pih_cost, sc1_be_cost, sc1_hic_cost, sc1_hfc_cost, sc2_p_cost, sc2_d_cost, sc2_dh_cost, sc2_pih_cost, sc2_be_cost, sc2_lpg_cost, sc2_cng_cost, sc3_d_cost, sc3_dh_cost, sc3_pih_cost, "
                        header = header & "sc3_be_cost, sc3_lpg_cost, sc3_cng_cost, sc3_hfc_cost, sc4_d_cost, sc4_dh_cost, sc4_hic_cost, sc4_hfc_cost, sc5_d_cost, sc5_dh_cost, sc5_hic_cost, sc5_hfc_cost, sc6_p_cost, sc6_be_cost, sc6_hfc_cost, "
                        header = header & "sc7_p_cost, sc7_d_cost, sc7_ph_cost, sc7_dh_cost, sc7_pih_cost, sc7_be_cost, sc7_hic_cost, sc7_hfc_cost, sc8_p_cost, sc8_d_cost, sc8_dh_cost, sc8_pih_cost, sc8_be_cost, sc8_lpg_cost, sc8_cng_cost, "
                        header = header & "sc9_d_cost, sc9_dh_cost, sc9_pih_cost, sc9_be_cost, sc9_lpg_cost, sc9_cng_cost, sc9_hfc_cost, sc10_d_cost, sc10_dh_cost, sc10_hic_cost, sc10_hfc_cost, sc11_d_cost, sc11_dh_cost, sc11_hic_cost, sc11_hfc_cost, "
                        header = header & "sc12_d_cost, sc12_dh_cost, sc12_hfc_cost, sc13_p_cost, sc13_d_cost, sc13_ph_cost, sc13_dh_cost, sc13_pih_cost, sc13_be_cost, sc13_hic_cost, sc13_hfc_cost, sc14_p_cost, sc14_d_cost, sc14_dh_cost, sc14_pih_cost, "
                        header = header & "sc14_be_cost, sc14_lpg_cost, sc14_cng_cost, sc15_d_cost, sc15_dh_cost, sc15_pih_cost, sc15_be_cost, sc15_lpg_cost, sc15_cng_cost, sc15_hfc_cost, sc16_d_cost, sc16_dh_cost, sc16_hic_cost, sc16_hfc_cost, sc17_d_cost, "
                        header = header & "sc17_dh_cost, sc17_hic_cost, sc17_hfc_cost, sc18_d_cost, sc18_dh_cost, sc18_hic_cost, sc18_hfc_cost, sc19_d_cost, sc19_dh_cost, sc19_hic_cost, sc19_hfc_cost "
                    Case "Temp Annual"
                        ToSQL = True
                        TableName = "TR_IO_RoadLink_Annual"
                        header = "modelrun_id, year, flow_id, m_lanes, d_lanes, s_lanes, m_lanes_new, d_lanes_new, s_lanes_new, msc1, msc2, msc3, msc4,  msc5, msc6, dsc1, dsc2, dsc3, dsc4, dsc5, dsc6, ssc1, ssc2, ssc3, ssc4, ssc5, ssc6, ssc7, ssc8,  max_cap_m, max_cap_d, max_cap_s, added_lane0, added_lane1, added_lane2"
                    Case "Temp Hourly"
                        ToSQL = True
                        TableName = "TR_IO_RoadLink_Hourly"
                        header = "modelrun_id, year, flow_id, hour_id, m_road_flows, d_road_flows, s_road_flows,"
                        For x = 1 To 6
                            header = header & ", m" & x & "hourly_flows" & "," & "m" & x & "_charge" & "," & "m" & x & "_latent_flows" & "," & "m" & x & "_new_hourly_speeds"
                        Next
                        For x = 1 To 6
                            header = header & ", d" & x & "hourly_flows" & "," & "d" & x & "_charge" & "," & "m" & x & "_latent_flows" & "," & "d" & x & "_new_hourly_speeds"
                        Next
                        For x = 1 To 8
                            header = header & ", s" & x & "hourly_flows" & "," & "s" & x & "_charge" & "," & "s" & x & "_latent_flows" & "," & "s" & x & "_new_hourly_speeds"
                        Next
                    Case "Temp"
                        TempFileName = FilePrefix & "RoadLinkTemp.csv"
                        header = "Yeary,FlowID,MLanes,DLanes,SLanes,MLanesNew,DLanesNew,SLanesNew,MSC1,MSC2,MSC3,MSC4,MSC5,MSC6,DSC1,DSC2,DSC3,DSC4,DSC5,DSC6,SSC1,SSC2,SSC3,SSC4,SSC5,SSC6,SSC7,SSC8,PopZ1,PopZ2,GVAZ1,GVAZ2,"
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
                End Select
            Case "RoadZone"
                Select Case SubType
                    Case "Fuel"
                        ToSQL = True
                        TableName = "TR_O_RoadZoneFuelConsumption"
                        OutFileName = FilePrefix & "RoadZoneFuelConsumption.csv"
                        header = "modelrun_id,  zone_id, year, pet_car, pet_lgv, die_car, die_lgv, die_hgv23, die_hgv4, die_psv, ele_car, ele_lgv, ele_psv, lpg_lgv, lpg_psv, cng_lgv, cng_psv, hyd_car, hyd_hgv23, hyd_hgv4, hyd_psv"
                    Case "NewCap"
                        TableName = "TR_O_RoadZoneNewCapacity"
                        OutFileName = FilePrefix & "RoadZoneNewCap.csv"
                        header = "modelrun_id, zone_id,changeyear,mway_cap,rur_a_cap,rur_m_cap,urb_cap"
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_RoadZoneOutputData"
                        OutFileName = FilePrefix & "RoadZoneOutputData.csv"
                        header = "modelrun_id,  zone_id, country_id, year, v_km, speed, petrol, diesel, electric, lpg, cng, hydrogen, mway_vkm, rur_a_vkm, rur_m_vkm, urb_vkm, mway_spd, rur_a_spd, rur_m_spd, urb_spd, pet_vkm, die_vkm, ph_vkm, dh_vkm, peh_vkm, e_vkm, lpg_vkm, cng_vkm, hyd_vkm, fc_vkm"
                    Case "Temp"
                        ToSQL = True
                        TableName = "TR_IO_RoadZone"
                        OutFileName = FilePrefix & "RoadZoneTemp.csv"
                        header = "modelrun_id, year, zone_id, speed, vkm, vkm_mway, rv_cat_traf_1_1, rv_cat_traf_1_2, rv_cat_traf_1_3, rv_cat_traf_1_4,  rv_cat_traf_1_5, vkm_rur_a, rv_cat_traf_2_1, rv_cat_traf_2_2, rv_cat_traf_2_3, rv_cat_traf_2_4,  rv_cat_traf_2_5, vkm_rur_m, rv_cat_traf_3_1, rv_cat_traf_3_2, rv_cat_traf_3_3, rv_cat_traf_3_4,  rv_cat_traf_3_5, vkm_urb, rv_cat_traf_4_1, rv_cat_traf_4_2, rv_cat_traf_4_3, rv_cat_traf_4_4,  rv_cat_traf_4_5, supresd_traffic_1_1, supresd_traffic_1_2, supresd_traffic_1_3, supresd_traffic_1_4,  supresd_traffic_2_1, supresd_traffic_2_2, supresd_traffic_2_3, supresd_traffic_2_4, supresd_traffic_3_1, supresd_traffic_3_2, supresd_traffic_3_3, supresd_traffic_3_4,  supresd_traffic_4_1, supresd_traffic_4_2, supresd_traffic_4_3, supresd_traffic_4_4, spd_mway,  spd_rur_a, spd_rur_m, spd_urb" 'LatentVkm1, LatentVkm2, LatentVkm3, LatentVkm4, AddedLaneKm1, AddedLaneKm2, AddedLaneKm3, AddedLaneKm4, BuiltLaneKm1, BuiltLaneKm2, BuiltLaneKm3, BuiltLaneKm4"
                    Case "ExtVar"
                        ToSQL = True
                        TableName = "TR_IO_RoadZoneExternalVariables"
                        OutFileName = EVFilePrefix & "RoadZoneExtVar.csv"
                        header = "modelrun_id, zone_id, Year, pop_z, gva_z, cost, stations, car_fuel, new_trips, gjt, elp"
                        'header = "modelrun_id, zone_id, year, pop_z, gva_z, cost, lane_km, mway_lkm, rur_ad_lkm, rur_as_lkm, rur_min_lkm, urb_d_lkm, urb_s_lkm,  p_car, d_car, e_car, p_lgv, d_lgv, e_lgv, d_hgv, e_hgv, d_psv, e_psv, p_bike, e_bike, fc_bike, ph_car, dh_car, pe_car, h_car, fc_car, dh_lgv, pe_lgv, l_lgv, c_lgv, dh_psv, pe_psv, l_psv, c_psv, fc_psv, dh_hgv, h_hgv, fc_hgv, lgv_cost, hgv1_cost, hgv2_cost, psv_cost, p_car_cost,d_car_cost, ph_car_cost, dh_car_cost, phper_car, pih_car_cost, be_car_cost, hic_car_cost, hfc_car_cost, p_lgv_cost, d_lgv_cost, dh_lgv_cost, phper_lgv, pih_lgv_cost, be_lgv_cost, lpg_lgv_cost, cng_lgv_cost, d_hgv1_cost, dh_hgv1_cost, hic_hgv1_cost, hfc_hgv1_cost, d_hgv2_cost, dh_hgv2_cost, hic_hgv2_cost, hfc_hgv2_cost, d_psv_cost,dh_psv_cost, phper_psv, pih_psv_cost, be_psv_cost, lgp_psv_cost, cng_psv_cost, hfc_psv_cost"
                    Case "CapChange"
                        ToSQL = True
                        TableName = "TR_IO_RoadZoneCapacityChange"
                        OutFileName = EVFilePrefix & "RoadZoneCapChange.csv"
                        header = "modelrun_id, zone_id, changeyear, mway_lane_kmch, rur_ad_lane_kmch, rur_as_lane_kmch, rur_m_lane_kmch, urb_d_lane_kmch, urb_s_lane_kmch"
                End Select
            Case "Seaport"
                Select Case SubType
                    Case "NewCap"
                        ToSQL = True
                        TableName = "TR_O_SeaFreightNewCapacity"
                        OutFileName = EVFilePrefix & "SeaFreightNewCap.csv"
                        header = "modelrun_id, portID, changeyear, new_lb_cap, new_db_cap, new_gc_cap, new_ll_cap, new_rr_cap"
                    Case "NewCap_Add"
                        TableName = "TR_O_SeaFreightNewCapacity_Add"
                        OutFileName = FilePrefix & "SeaNewCap.csv"
                        header = "modelrun_id, portID, changeyear, lb_cap_added,db_cap_added,gc_cap_added,ll_cap_added,rr_cap_added"
                    Case "Output"
                        ToSQL = True
                        TableName = "TR_O_SeaFreightOutputData"
                        OutFileName = FilePrefix & "SeaOutputData.csv"
                        header = "modelrun_id, port_id, year, liq_blk, dry_blk, g_cargo, lo_lo, ro_ro, gas_oil, fuel_oil"
                    Case "Temp"
                        ToSQL = True
                        TableName = "TR_IO_SeaFreight"
                        OutFileName = FilePrefix & "SeaTemplate.csv"
                        header = "modelrun_id, year, port_id, liq_blk, dry_blk, gc_rgo, lo_lo, ro_ro, added_cap_1, added_cap_2, added_cap_3, added_cap_4, added_cap_5"
                    Case "ExtVar"
                        ToSQL = True
                        TableName = "TR_IO_SeaFreightExternalVariables"
                        OutFileName = EVFilePrefix & "SeaFreightExtVar.csv"
                        header = "modelrun_id, port_id, year, lb_cap, db_cap, gc_cap, ll_cap, rr_cap, gor_pop,gor_gva, cost, fuel_eff"
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
            Dim aryFields As String()
            aryFields = header.Split(",")
            For iy = 0 To UBound(aryFields, 1)
                aryFieldNames.Add(UnNull(aryFields(iy), VariantType.String))
            Next
            'use array data for field values
            For iy = 0 To UBound(OutputArray, 1)
                aryFieldValues.Clear()
                'exit if write to the end of the data
                'size of outputarray must be zone/flow number + 1
                If OutputArray(iy + 1, 0) Is Nothing Then
                    'changed from 0 to 1, as the modelrun_id is dummy and empty in the database for now
                    'If OutputArray(iy, 1) Is Nothing Then
                    Exit For
                End If
                For ix = 0 To UBound(OutputArray, 2)
                    'For ix = 1 To UBound(OutputArray, 2)
                    aryFieldValues.Add(UnNull(OutputArray(iy + 1, ix), VariantType.String))
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
                    strSQL_N &= Chr(34) & Trim(aryFieldNames(i)) & Chr(34) & ", "
                Next
                'Get rid of the last comma and space
                strSQL_N = Left(strSQL_N, Len(strSQL_N) - 2)

                'Get a list of Field Values
                For i = 0 To aryFieldNames.Count - 1
                    strSQL_V &= Trim(aryFieldValues.Item(i)) & ", "
                Next
                'Get rid of the last comma and space
                strSQL_V = Left(strSQL_V, Len(strSQL_V) - 2)

                strSQL_All = "INSERT INTO " & Chr(34) & TableName & Chr(34) & " (" & strSQL_N & ") "
                strSQL_All = strSQL_All & "VALUES (" & strSQL_V & ")"
            Else
                strSQL_NV = ""
                'Get a list of Field Names = Values
                For i = 0 To aryFieldNames.Count - 1
                    strSQL_NV &= aryFieldNames.Item(i) & " = " & aryFieldValues.Item(i) & ", "
                Next
                'Get rid of the last comma and space
                strSQL_NV = Left(strSQL_NV, Len(strSQL_NV) - 2)

                strSQL_All = "UPDATE " & Chr(34) & TableName & Chr(34) & " SET " & strSQL_NV
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
            'Throw ex
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

    Public Sub CloseLog()
        logarray(logNum, 0) = "Model run finished at " & System.DateTime.Now
        logNum += 1
        logarray(logNum, 0) = "Code written by Dr Simon Blainey, Transportation Research Group, University of Southampton"
        logNum += 1
        logarray(logNum, 0) = "All results are indicative estimates, and the authors accept no liability for any actions arising from the use of these results"
        logNum += 1

        Call WriteData("Logfile", "", logarray)
    End Sub

End Module
