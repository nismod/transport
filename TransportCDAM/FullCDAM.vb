Imports System.ComponentModel

Public Class FullCDAM : Implements IDisposable
    'this version incorporates EV input from the database.

    Public Function runCDAM(ByVal ModelRun_ID As Integer, ByVal Model_Year As Integer, ByVal Dbase As String) As Boolean
        Try

            'Store global variables
            g_modelRunID = ModelRun_ID
            g_modelRunYear = Model_Year
            g_dbase = Dbase
            'Setup global LogFile variant types
            g_LogVTypes(0) = VariantType.Integer : g_LogVTypes(1) = VariantType.Integer : g_LogVTypes(2) = VariantType.String

            'Get Model Run Details including
            If getModelRunDetails() = False Then
                Throw New System.Exception("Error getting Model Run details from database")
            End If

            'Get Model run parameters
            GetParameters()

            'Run Transport Model
            FullMain()

            Me.Finalize()
            Return True

        Catch ex As Exception
            Dim msg As String = "Fatal Error in Code - Transport Model stopped"
            If ex.Message <> "" Then msg = ex.Message
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = msg
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
            Throw New System.Exception(msg)
            Me.Finalize()
            Return False
        End Try
    End Function

    Private Function getModelRunDetails() As Boolean
        Dim mrdarray As String(,) = Nothing

        Call DBaseInterface.ReadData("System", "ModelRunDetails", mrdarray)

        Return True

    End Function

    Public Sub GetParameters()
        Dim ary As String(,) = Nothing
        Dim i As Integer
        Dim ParamName As String

        'get plan details from the database
        DBaseInterface.ReadData("Inputs", "Parameters", ary)

        For i = 1 To UBound(ary, 1) - 1
            ParamName = CStr(ary(i, 3))
            Select Case ParamName
                Case "RunRoadLink"
                    RunRoadLink = CBool(ary(i, 5))
                Case "RunRoadZone"
                    RunRoadZone = CBool(ary(i, 5))
                Case "RunRailLink"
                    RunRailLink = CBool(ary(i, 5))
                Case "RunRailZone"
                    RunRailZone = CBool(ary(i, 5))
                Case "RunAir"
                    RunAir = CBool(ary(i, 5))
                Case "RunSea"
                    RunSea = CBool(ary(i, 5))
                Case "BuildInfra"
                    BuildInfra = CBool(ary(i, 5))
                Case "CUCritValue"
                    CUCritValue = CDbl(ary(i, 5))
                Case "VariableEl"
                    VariableEl = CBool(ary(i, 5))
                Case "ElCritValue"
                    ElCritValue = CDbl(ary(i, 5)) / 100
                Case "CongestionCharge"
                    CongestionCharge = CBool(ary(i, 5))
                Case "ConChargeYear"
                    ConChargeYear = CInt(ary(i, 5))
                Case "ConChargePer"
                    ConChargePer = CDbl(ary(i, 5)) / 100
                Case "CarbonCharge"
                    CarbonCharge = CBool(ary(i, 5))
                Case "CarbChargeYear"
                    CarbChargeYear = CInt(ary(i, 5))
                Case "WPPL"
                    WPPL = CBool(ary(i, 5))
                Case "WPPLYear"
                    WPPLYear = CInt(ary(i, 5))
                Case "WPPLPer"
                    WPPLPer = CDbl(ary(i, 5)) / 100
                Case "RailCCharge"
                    RailCCharge = CBool(ary(i, 5))
                Case "RlCChargeYear"
                    RlCChargeYear = CInt(ary(i, 5))
                Case "RailChargePer"
                    RailChargePer = CDbl(ary(i, 5)) / 100
                Case "RlCaCharge"
                    RlCaCharge = CBool(ary(i, 5))
                Case "RlCaChYear"
                    RlCaChYear = CDbl(ary(i, 5))
                Case "AirCCharge"
                    AirCCharge = CBool(ary(i, 5))
                Case "AirChargeYear"
                    AirChargeYear = CInt(ary(i, 5))
                Case "AirChargePer"
                    AirChargePer = CDbl(ary(i, 5)) / 100
                Case "AirCaCharge"
                    AirCaCharge = CBool(ary(i, 5))
                Case "AirCaChYear"
                    AirCaChYear = CInt(ary(i, 5))
                Case "SmarterChoices"
                    SmarterChoices = CBool(ary(i, 5))
                Case "SmartIntro"
                    SmartIntro = CInt(ary(i, 5))
                Case "SmartPer"
                    SmartPer = CDbl(ary(i, 5)) / 100
                Case "SmartYears"
                    SmartYears = CInt(ary(i, 5))
                Case "SmartFrt"
                    SmartFrt = CBool(ary(i, 5))
                Case "SmFrtIntro"
                    SmFrtIntro = CInt(ary(i, 5))
                Case "SmFrtPer"
                    SmFrtPer = CDbl(ary(i, 5)) / 100
                Case "SmFrtYears"
                    SmFrtYears = CInt(ary(i, 5))
                Case "UrbanFrt"
                    UrbanFrt = CBool(ary(i, 5))
                Case "UrbFrtIntro"
                    UrbFrtIntro = CInt(ary(i, 5))
                Case "UrbFrtPer"
                    UrbFrtPer = CDbl(ary(i, 5)) / 100
                Case "UrbFrtYears"
                    UrbFrtYears = CInt(ary(i, 5))
                Case "NewRdLEV"
                    NewRdLEV = CBool(ary(i, 5))
                Case "NewRdZEV"
                    NewRdZEV = CBool(ary(i, 5))
                Case "NewRlLEV"
                    NewRlLEV = CBool(ary(i, 5))
                Case "NewRlZEV"
                    NewRlZEV = CBool(ary(i, 5))
                Case "NewAirEV"
                    NewAirEV = CBool(ary(i, 5))
                Case "NewSeaEV"
                    NewSeaEV = CBool(ary(i, 5))
                Case "NewRoadLanes"
                    NewRoadLanes = CDbl(ary(i, 5))
                Case "NewRailTracks"
                    NewRailTracks = CDbl(ary(i, 5))
                Case "NewRlZCap"
                    NewRlZCap = CBool(ary(i, 5))
                Case "NewAirRun"
                    NewAirRun = CDbl(ary(i, 5))
                Case "NewAirTerm"
                    NewAirTerm = CDbl(ary(i, 5))
                Case "NewSeaCap"
                    NewSeaCap = CBool(ary(i, 5))
                Case "NewSeaTonnes"
                    NewSeaTonnes = CDbl(ary(i, 5))
                Case "RlElect"
                    RlElect = CBool(ary(i, 5))
                Case "ElectKmPerYear"
                    ElectKmPerYear = CDbl(ary(i, 5))
                Case "NewRdLCap"
                    NewRdLCap = CBool(ary(i, 5))
                Case "NewRdZCap"
                    NewRdZCap = CBool(ary(i, 5))
                Case "NewRlLCap"
                    NewRlLCap = CBool(ary(i, 5))
                Case "NewAirCap"
                    NewAirCap = CBool(ary(i, 5))
                Case "RailCUPeriod"
                    RailCUPeriod = CStr(ary(i, 5))
                Case "RlPeakHeadway"
                    RlPeakHeadway = CDbl(ary(i, 5))
                Case "RdZSpdSource"
                    RdZSpdSource = CStr(ary(i, 5))
                Case "TripRates"
                    TripRates = CBool(ary(i, 5))
                Case "RlZOthSource"
                    RlZOthSource = CBool(ary(i, 5))
                Case "RlLOthSource"
                    RlLOthSource = CBool(ary(i, 5))
                Case "CapChangeID"
                    CapChangeID = CInt(ary(i, 5))
                Case Else
                    Dim msg As String
                    msg = "The following parameter was found in the Parameters table but was not handled by the code: " & ParamName
                    Call ErrorLog(ErrorSeverity.WARNING, "FullCDAM", "FullMain", msg)
                    'Stop
                    '....
            End Select
        Next

    End Sub

    Sub FullMain()
        Dim y As Integer

        'get directory path for files - **now unnecessary as set by user
        'DirPath = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Inputs\"

        'creates the log file for the model
        Call CreateLog()

        'get strategy variables for this year and for last year from the database
        If g_modelRunYear = 2010 Then
            Call DBaseInterface.ReadData("SubStrategy", "", stratarrayOLD, 2010, g_modelRunID)
            Call DBaseInterface.ReadData("SubStrategy", "", stratarray, 2011, g_modelRunID)

        Else
            Call DBaseInterface.ReadData("SubStrategy", "", stratarrayOLD, g_modelRunYear - 1, g_modelRunID)
            Call DBaseInterface.ReadData("SubStrategy", "", stratarray, g_modelRunYear, g_modelRunID)
        End If

        'Get energy data
        'Call DBaseInterface.ReadData("Energy", "", enearray)
        'If g_modelRunYear <> 2010 Then
        '    enearray(1, 1) = enearray(g_modelRunYear - 2010, 1) 'petrol
        '    enearray(1, 2) = enearray(g_modelRunYear - 2010, 2) 'diesel
        '    enearray(1, 3) = enearray(g_modelRunYear - 2010, 3) 'electricity
        '    enearray(1, 4) = 1 'LPG
        '    enearray(1, 5) = 1 'CNG
        '    enearray(1, 6) = 1 'hydrogen
        '    enearray(2, 1) = enearray(g_modelRunYear - 2009, 1) 'petrol
        '    enearray(2, 2) = enearray(g_modelRunYear - 2009, 2) 'diesel
        '    enearray(2, 3) = enearray(g_modelRunYear - 2009, 3) 'electricity
        '    enearray(2, 4) = 1 'LPG
        '    enearray(2, 5) = 1 'CNG
        '    enearray(2, 6) = 1 'hydrogen
        'End If
        'read fuel price for previous year (1,x) and current year (2,x)
        Call get_fuelprice_by_modelrun_id(g_modelRunID, g_modelRunYear, 0)
        If RlZEneSource = "Database" Then
            'If g_modelRunYear = 2010 Then y = 1 Else y = g_modelRunYear - g_initialYear + 1 'TODO - this needs fixing once we go to database for energy
            InDieselOldAll = enearray(1, 2)
            InElectricOldAll = enearray(1, 3)
            InDieselNewAll = enearray(2, 2)
            InElectricNewAll = enearray(2, 3)
        End If

        'lookup the combination of the capacity change
        Call CapChangeComb()

        'check if creating external variable files
        If CreateExtVars = True Then
            Call ExtVarMain()
        End If

        'check if adding capacity
        If UpdateExtVars = True Then
            Call UpdateExtVarFiles()
        End If

        'check if running model, and if not then close log file and end
        If RunModel = False Then
            Call CloseLog()
            Dim msg As String = "RunModel = False"
            Call ErrorLog(ErrorSeverity.FATAL, "FullCDAM", "FullMain", msg)
            Throw New System.Exception(msg)
        End If

        'add model elements to log file
        Call ModelElementLog()

        'if RoadLink model is selected then run that model
        If RunRoadLink = True Then
            Call RoadLinkMain()
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Road link model run completed"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If

        'if RoadZone model is selected then run that model
        If RunRoadZone = True Then
            'modification to allow use of old or new speed calculations
            If RdZSpdSource = "Elasticity" Then
                Call RoadZoneMain()
            Else
                Call RoadZoneMainNew()
            End If
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Road zone model run completed"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If

        'if RailLink model is selected then run that model
        If RunRailLink = True Then
            Call RailLinkMain()
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Rail link model run completed"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If

        'if RailZone model is selected then run that model
        If RunRailZone = True Then
            Call RailZoneMain()
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Rail zone model run completed"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If

        'if Air model is selected then run that model
        If RunAir = True Then
            Call AirMain()
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Air model run completed"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If

        'if Sea model is selected then run that model
        If RunSea = True Then
            Call SeaMain()
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Sea model run completed"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If

        'write to cross sector output
        Call WriteCrossSectorOutput()

        'Write closing lines of log file
        Call CloseLog()

    End Sub

    Sub CreateLog()
        'write header rows
        logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "ITRC Transport CDAM"
        Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
    End Sub

    Sub ExtVarMain()
        Dim dbline As String
        Dim dbpoparray(), dbecoarray() As String
        Dim zoneval As Long
        Dim popval, ecoval As Double
        Dim RdLInputFile, RdZInputFile, RlLInputFile, RlZInputFile, AirFInputFile, AirNInputFile, SeaInputFile As IO.FileStream
        Dim rlif, rzif, rllif, rlzif, afif, anif, sif As IO.StreamReader
        Dim RdLInputFileNew, RdZInputFileNew, RlLInputFileNew, RlZInputFileNew, AirFInputFileNew, AirNInputFileNew, SeaInputFileNew As IO.FileStream
        Dim rlin, rzin, rllin, rlzin, afin, anin, sin As IO.StreamWriter
        Dim inline, outline As String
        Dim inarray() As String
        Dim arraycount As Integer
        Dim newval As Double
        Dim dictkey As String
        Dim dictyear As Long
        Dim newcosts As Double
        Dim electricprop As Double
        Dim airnewcost As Double

        'write to log file first
        logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "The following external variable files were generated:"
        Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        If NewRdLEV = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Road link external variables"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If NewRdZEV = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Road zone external variables"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If NewRlLEV = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Rail link external variables"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If NewRlZEV = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Rail zone external variables"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If NewAirEV = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Airport external variables"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If NewSeaEV = True Then
            'Call SeaEVMain()
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Seaport external variables"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If

        'if we are using database files then loop through transformed database file to start year (2010)
        If DBasePop = True Then
            ZonePopFile = New IO.FileStream(DirPath & "ZoneScenarioPopFile.csv", IO.FileMode.Open, IO.FileAccess.Read)
            zpr = New IO.StreamReader(ZonePopFile, System.Text.Encoding.Default)
            'read header row
            dbline = zpr.ReadLine
            'loop through file until we get to 2010
            Do
                dbline = zpr.ReadLine
                dbpoparray = Split(dbline, ",")
                If dbpoparray(0) = "2010" Then
                    Exit Do
                End If
            Loop
        End If
        If DBaseEco = True Then
            ZoneEcoFile = New IO.FileStream(DirPath & "ZoneScenarioEcoFile.csv", IO.FileMode.Open, IO.FileAccess.Read)
            zer = New IO.StreamReader(ZoneEcoFile, System.Text.Encoding.Default)
            'read header row
            dbline = zer.ReadLine
            'loop through file until we get to 2010
            Do
                dbline = zer.ReadLine
                dbecoarray = Split(dbline, ",")
                If dbecoarray(0) = "2010" Then
                    Exit Do
                End If
            Loop
        End If
        If DBaseEne = True Then

            '****need to add in energy cost file input
        End If
        'once we get to first row for 2010, load the 2010 values into a dictionary, and then update all the model input files
        'load 2010 pop data into pop dictionary
        If DBasePop = True Then
            Do
                If dbpoparray(0) = "2011" Then
                    Exit Do
                Else
                    zoneval = dbpoparray(1)
                    popval = dbpoparray(2)
                    PopLookup.Add(zoneval, popval)
                    dbline = zpr.ReadLine
                    dbpoparray = Split(dbline, ",")
                End If
            Loop
        End If
        'load 2010 economy data into eco dictionary
        If DBaseEco = True Then
            Do
                If dbecoarray(0) = "2011" Then
                    Exit Do
                Else
                    zoneval = dbecoarray(1)
                    ecoval = dbecoarray(2)
                    EcoLookup.Add(zoneval, ecoval)
                    dbline = zer.ReadLine
                    dbecoarray = Split(dbline, ",")
                End If
            Loop
        End If

        'load 2010 energy data into energy dictionary
        If DBaseEne = True Then
            '***need to add in energy dictionary
        End If
        'update input files assuming that they need updating
        'update the initial file if the box has been ticked, otherwise use the existing input file
        If UpdateInput = True Then
            If DBaseCheck = True Then
                If NewRdLEV = True Then
                    'road link input file
                    RdLInputFile = New IO.FileStream(DirPath & "RoadInputDataInitial.csv", IO.FileMode.Open, IO.FileAccess.Read)
                    rlif = New IO.StreamReader(RdLInputFile, System.Text.Encoding.Default)
                    RdLInputFileNew = New IO.FileStream(DirPath & "RoadInputData2010.csv", IO.FileMode.Create, IO.FileAccess.Write)
                    rlin = New IO.StreamWriter(RdLInputFileNew, System.Text.Encoding.Default)
                    'read and write header line
                    inline = rlif.ReadLine
                    rlin.WriteLine(inline)
                    'update the remainder of the lines as necessary
                    Do
                        inline = rlif.ReadLine
                        If inline Is Nothing Then
                            Exit Do
                        End If
                        inarray = Split(inline, ",")
                        OZone = inarray(1)
                        DZone = inarray(2)
                        arraycount = 0
                        outline = ""
                        Do While arraycount < 28
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        'if getting pop data from pop database then get values from dictionary, otherwise use old value
                        If DBasePop = True Then
                            If PopLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                            If PopLookup.TryGetValue(DZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                        Else
                            outline = outline & inarray(28) & "," & inarray(29) & ","
                        End If
                        'if getting economy data from database then get values from dictionary, otherwise use old value
                        If DBaseEco = True Then
                            If EcoLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                            If EcoLookup.TryGetValue(DZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                        Else
                            outline = outline & inarray(30) & "," & inarray(31) & ","
                        End If
                        'if getting energy cost data from database then get values from dictionary, otherwise use old value
                        If DBaseEne = True Then
                            newval = 26.604
                            outline = outline & newval & ","
                            'other costs go at the end due to their place in the file
                        Else
                            outline = outline & inarray(32) & ","
                        End If
                        arraycount = 33
                        Do While arraycount < 36
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        If DBaseEne = True Then
                            newval = (0.598 * 36.14) + (0.402 * 36.873)
                            outline = outline & newval & ","
                            newval = 61.329
                            outline = outline & newval & ","
                            newval = 234.5
                            outline = outline & newval & ","
                            newval = 93.665
                            outline = outline & newval & ","
                            newval = 109.948
                            outline = outline & newval & ","
                            newval = 26.604
                            outline = outline & newval & ","
                            newval = (0.598 * 36.14) + (0.402 * 36.873)
                            outline = outline & newval & ","
                            newval = 61.329
                            outline = outline & newval & ","
                            newval = 234.5
                            outline = outline & newval & ","
                            newval = 93.665
                            outline = outline & newval & ","
                            newval = 109.948
                            outline = outline & newval & ","
                            newval = 26.604
                            outline = outline & newval & ","
                            newval = (0.598 * 36.14) + (0.402 * 36.873)
                            outline = outline & newval & ","
                            newval = 61.329
                            outline = outline & newval & ","
                            newval = 234.5
                            outline = outline & newval & ","
                            newval = 93.665
                            outline = outline & newval & ","
                            newval = 93.665
                            outline = outline & newval & ","
                            newval = 109.948
                            outline = outline & newval & ","
                            newval = 109.948
                            outline = outline & newval & ","
                        Else
                            arraycount = 36
                            Do While arraycount < 55
                                outline = outline & inarray(arraycount) & ","
                                arraycount += 1
                            Loop
                        End If
                        rlin.WriteLine(outline)
                    Loop
                    rlif.Close()
                    rlin.Close()
                End If

                If NewRdZEV = True Then
                    'road zone input file
                    RdZInputFile = New IO.FileStream(DirPath & "RoadZoneInputDataInitial.csv", IO.FileMode.Open, IO.FileAccess.Read)
                    rzif = New IO.StreamReader(RdZInputFile, System.Text.Encoding.Default)
                    RdZInputFileNew = New IO.FileStream(DirPath & "RoadZoneInputData2010.csv", IO.FileMode.Create, IO.FileAccess.Write)
                    rzin = New IO.StreamWriter(RdZInputFileNew, System.Text.Encoding.Default)
                    'read and write header line
                    inline = rzif.ReadLine
                    rzin.WriteLine(inline)
                    Do
                        inline = rzif.ReadLine
                        If inline Is Nothing Then
                            Exit Do
                        End If
                        inarray = Split(inline, ",")
                        OZone = inarray(0)
                        arraycount = 0
                        outline = ""
                        Do While arraycount < 3
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        'if getting pop data from pop database then get values from dictionary, otherwise use old value
                        If DBasePop = True Then
                            If PopLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                        Else
                            outline = outline & inarray(3) & ","
                        End If
                        'if getting economy data from database then get values from dictionary, otherwise use old value
                        If DBaseEco = True Then
                            If EcoLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                        Else
                            outline = outline & inarray(4) & ","
                        End If
                        outline = outline + inarray(5) & ","
                        'if getting energy cost data from database then get values from dictionary, otherwise use old value
                        If DBaseEne = True Then
                            'in first year there are only petrol and diesel vehicles to take account of - 59.8% of cost variable from petrol vehicles and 40.2% from diesel cars
                            'car cost figure is 0.598*P + 0.402*D
                            newval = (0.598 * 36.14) + (0.402 * 36.873)
                            outline = outline & newval & ","
                            'need to do other cost figures at the end because they are at a different place in file
                        Else
                            outline = outline & inarray(6) & ","
                        End If
                        arraycount = 7
                        Do While arraycount < 18
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        'do remainder of the cost variables
                        If DBaseEne = True Then
                            'LGVCost
                            newval = 61.329
                            outline = outline & newval & ","
                            'HGV1Cost
                            newval = 93.665
                            outline = outline & newval & ","
                            'HGV2Cost
                            newval = 109.948
                            outline = outline & newval & ","
                            'PSVCost
                            newval = 234.5
                            outline = outline & newval & ","
                        Else
                            outline = outline & inarray(18) & "," & inarray(19) & "," & inarray(20) & "," & inarray(21) & ","
                        End If
                        For ac = 22 To 33
                            outline = outline & inarray(ac) & ","
                        Next
                        rzin.WriteLine(outline)
                    Loop
                    rzif.Close()
                    rzin.Close()
                End If

                If NewRlLEV = True Then
                    'rail link input file
                    'XUCHENG - IS THIS CODE USED? SHOULD THESE BE IN READDATA FUNCTION?
                    'Yes, this is to translate the raw data into the format for the calculation
                    'They will be removed if we have connection to the database in the next version
                    RlLInputFile = New IO.FileStream(DirPath & "RailLinkInputDataInitial.csv", IO.FileMode.Open, IO.FileAccess.Read)
                    rllif = New IO.StreamReader(RlLInputFile, System.Text.Encoding.Default)
                    RlLInputFileNew = New IO.FileStream(DirPath & "RailLinkInputData2010.csv", IO.FileMode.Create, IO.FileAccess.Write)
                    rllin = New IO.StreamWriter(RlLInputFileNew, System.Text.Encoding.Default)
                    'read and write header line
                    inline = rllif.ReadLine
                    rllin.WriteLine(inline)
                    Do
                        inline = rllif.ReadLine
                        If inline Is Nothing Then
                            Exit Do
                        End If
                        inarray = Split(inline, ",")
                        OZone = inarray(1)
                        DZone = inarray(2)
                        arraycount = 0
                        outline = ""
                        Do While arraycount < 5
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        'if getting pop data from pop database then get values from dictionary, otherwise use old value
                        If DBasePop = True Then
                            If PopLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                            If PopLookup.TryGetValue(DZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                        Else
                            outline = outline & inarray(5) & "," & inarray(6) & ","
                        End If
                        'if getting economy data from database then get values from dictionary, otherwise use old value
                        If DBaseEco = True Then
                            If EcoLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                            If EcoLookup.TryGetValue(DZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                        Else
                            outline = outline & inarray(7) & "," & inarray(8) & ","
                        End If
                        outline = outline & inarray(9) & ","
                        'if getting energy cost data from database then get values from dictionary, otherwise use old value
                        If DBaseEne = True Then
                            '***need to bring in car fuel costs from input data
                            'assumes base diesel cost per km is 29.204p and base electric cost per km is 16.156p, base diesel maintenance cost per km are 37.282p, base electric maintenance cost per km 24.855p
                            '...base other costs for both traction types are 121.381p
                            electricprop = inarray(13)
                            newcosts = 121.381 + (electricprop * 41.011) + ((1 - electricprop) * 66.486)
                            outline = outline & newcosts & "," & inarray(11) & ","
                        Else
                            outline = outline & inarray(10) & "," & inarray(11) & ","
                        End If
                        arraycount = 12
                        Do While arraycount < 18
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        rllin.WriteLine(outline)
                    Loop
                    rllif.Close()
                    rllin.Close()
                End If

                If NewRlZEV = True Then
                    'rail zone input file
                    'XUCHENG - IS THIS CODE USED? SHOULD THESE BE IN READDATA FUNCTION?
                    RlZInputFile = New IO.FileStream(DirPath & "RailZoneInputDataInitial.csv", IO.FileMode.Open, IO.FileAccess.Read)
                    rlzif = New IO.StreamReader(RlZInputFile, System.Text.Encoding.Default)
                    RlZInputFileNew = New IO.FileStream(DirPath & "RailZoneInputData2010.csv", IO.FileMode.Create, IO.FileAccess.Write)
                    rlzin = New IO.StreamWriter(RlZInputFileNew, System.Text.Encoding.Default)
                    'read and write header line
                    inline = rlzif.ReadLine
                    rlzin.WriteLine(inline)
                    Do
                        inline = rlzif.ReadLine
                        If inline Is Nothing Then
                            Exit Do
                        End If
                        inarray = Split(inline, ",")
                        OZone = inarray(0)
                        arraycount = 0
                        outline = ""
                        Do While arraycount < 4
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        'if getting pop data from pop database then get values from dictionary, otherwise use old value
                        If DBasePop = True Then
                            If PopLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                        Else
                            outline = outline & inarray(4) & ","
                        End If
                        'if getting economy data from database then get values from dictionary, otherwise use old value
                        If DBaseEco = True Then
                            If EcoLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                        Else
                            outline = outline & inarray(5) & ","
                        End If
                        'if getting energy cost data from database then get values from dictionary, otherwise use old value
                        If DBaseEne = True Then
                            '****need to bring in car fuel costs from input data, and note that we also need to add in stations even though it doesn't change****
                            'assumes base diesel cost per km is 29.204p and base electric cost per km is 16.156p, base diesel maintenance cost per km are 37.282p, base electric maintenance cost per km 24.855p
                            '...base other costs for both traction types are 121.381p
                            electricprop = inarray(11)
                            newcosts = 121.381 + (electricprop * 41.011) + ((1 - electricprop) * 66.486)
                            outline = outline & newcosts & "," & inarray(7) & "," & inarray(8) & ","
                        Else
                            outline = outline & inarray(6) & "," & inarray(7) & "," & inarray(8) & ","
                        End If
                        arraycount = 9
                        Do While arraycount < 13
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        rlzin.WriteLine(outline)
                    Loop
                    rlzif.Close()
                    rlzin.Close()
                End If

                If NewAirEV = True Then
                    'air flow input file
                    'XUCHENG - IS THIS CODE USED? SHOULD THESE BE IN READDATA FUNCTION?
                    AirFInputFile = New IO.FileStream(DirPath & "AirFlowInputDataInitial.csv", IO.FileMode.Open, IO.FileAccess.Read)
                    afif = New IO.StreamReader(AirFInputFile, System.Text.Encoding.Default)
                    AirFInputFileNew = New IO.FileStream(DirPath & "AirFlowInputData2010.csv", IO.FileMode.Create, IO.FileAccess.Write)
                    afin = New IO.StreamWriter(AirFInputFileNew, System.Text.Encoding.Default)
                    'read and write header line
                    inline = afif.ReadLine
                    afin.WriteLine(inline)
                    Do
                        inline = afif.ReadLine
                        If inline Is Nothing Then
                            Exit Do
                        End If
                        inarray = Split(inline, ",")
                        OZone = inarray(10)
                        DZone = inarray(11)
                        arraycount = 0
                        outline = ""
                        Do While arraycount < 4
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        'if getting pop data from pop database then get values from dictionary, otherwise use old value
                        If DBasePop = True Then
                            If PopLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                            If PopLookup.TryGetValue(DZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                        Else
                            outline = outline & inarray(4) & "," & inarray(5) & ","
                        End If
                        'if getting economy data from database then get values from dictionary, otherwise use old value
                        If DBaseEco = True Then
                            If EcoLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                            If EcoLookup.TryGetValue(DZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                        Else
                            outline = outline & inarray(6) & "," & inarray(7) & ","
                        End If
                        'if getting energy cost data from database then get values from dictionary, otherwise use old value
                        If DBaseEne = True Then
                            'cost = flowdist * domesticplanesize * fuelperseatkm * cost per litre / 0.29 '(which is fuel percentage of total)
                            airnewcost = (inarray(9) * 89 * 0.037251 * 49.218) / 0.29
                            outline = outline & airnewcost & ","
                        Else
                            outline = outline & inarray(8) & ","
                        End If
                        arraycount = 9
                        Do While arraycount < 12
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        afin.WriteLine(outline)
                    Loop
                    afif.Close()
                    afin.Close()
                    'air node input file
                    'XUCHENG - IS THIS CODE USED? SHOULD THESE BE IN READDATA FUNCTION?
                    AirNInputFile = New IO.FileStream(DirPath & "AirNodeInputDataInitial.csv", IO.FileMode.Open, IO.FileAccess.Read)
                    anif = New IO.StreamReader(AirNInputFile, System.Text.Encoding.Default)
                    AirNInputFileNew = New IO.FileStream(DirPath & "AirNodeInputData2010.csv", IO.FileMode.Create, IO.FileAccess.Write)
                    anin = New IO.StreamWriter(AirNInputFileNew, System.Text.Encoding.Default)
                    'read and write header line
                    inline = anif.ReadLine
                    anin.WriteLine(inline)
                    Do
                        inline = anif.ReadLine
                        If inline Is Nothing Then
                            Exit Do
                        End If
                        inarray = Split(inline, ",")
                        OZone = inarray(14)
                        arraycount = 0
                        outline = ""
                        Do While arraycount < 6
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        'if getting pop data from pop database then get values from dictionary, otherwise use old value
                        If DBasePop = True Then
                            If PopLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                        Else
                            outline = outline & inarray(6) & ","
                        End If
                        'if getting economy data from database then get values from dictionary, otherwise use old value
                        If DBaseEco = True Then
                            If EcoLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                        Else
                            outline = outline & inarray(7) & ","
                        End If
                        'if getting energy cost data from database then get values from dictionary, otherwise use old value
                        If DBaseEne = True Then
                            'cost = averagedist * intplanesize * fuelperseatkm * cost per litre / 0.29 '(which is fuel percentage of total)
                            airnewcost = (inarray(13) * 155 * 0.037251 * 49.218) / 0.29
                            outline = outline & airnewcost & ","
                        Else
                            outline = outline & inarray(8) & ","
                        End If
                        arraycount = 9
                        Do While arraycount < 15
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        anin.WriteLine(outline)
                    Loop
                    anif.Close()
                    anin.Close()
                End If
                If NewSeaEV = True Then
                    'seaport input file
                    'XUCHENG - IS THIS CODE USED? SHOULD THESE BE IN READDATA FUNCTION?
                    SeaInputFile = New IO.FileStream(DirPath & "SeaFreightInputDataInitial.csv", IO.FileMode.Open, IO.FileAccess.Read)
                    sif = New IO.StreamReader(SeaInputFile, System.Text.Encoding.Default)
                    SeaInputFileNew = New IO.FileStream(DirPath & "SeaFreightInputData.csv", IO.FileMode.Create, IO.FileAccess.Write)
                    sin = New IO.StreamWriter(SeaInputFileNew, System.Text.Encoding.Default)
                    'read and write header line
                    inline = sif.ReadLine
                    sin.WriteLine(inline)
                    Do
                        inline = sif.ReadLine
                        If inline Is Nothing Then
                            Exit Do
                        End If
                        inarray = Split(inline, ",")
                        OZone = inarray(14)
                        arraycount = 0
                        outline = ""
                        Do While arraycount < 11
                            outline = outline & inarray(arraycount) & ","
                            arraycount += 1
                        Loop
                        'if getting pop data from pop database then get values from dictionary, otherwise use old value
                        If DBasePop = True Then
                            If PopLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("population")
                            End If
                        Else
                            outline = outline & inarray(11) & ","
                        End If
                        'if getting economy data from database then get values from dictionary, otherwise use old value
                        If DBaseEco = True Then
                            If EcoLookup.TryGetValue(OZone, newval) Then
                                outline = outline & newval & ","
                            Else
                                Call MissingValueError("gva")
                            End If
                        Else
                            outline = outline & inarray(12) & ","
                        End If
                        'if getting energy cost data from database then get values from dictionary, otherwise use old value
                        If DBaseEne = True Then
                            '****need to add this in ****
                            outline = outline & inarray(13) & ","
                        Else
                            outline = outline & inarray(13) & ","
                        End If
                        outline = outline & inarray(14) & ","
                        sin.WriteLine(outline)
                    Loop
                    sif.Close()
                    sin.Close()
                End If
            End If
        End If
        'once model input files have been updated, then go on to 2011, loop through every year and update/create (probably create) model EV files as we go along
        'but can't loop by year, so create a compound dictionary containing all values for population for years 1 to 90
        'population
        If DBasePop = True Then
            'clear existing pop lookup array
            PopLookup.Clear()
            'loop through
            '***note that at present population forecasts are only produced up to 2093
            Do
                dictyear = dbpoparray(0) - 2010
                dictkey = dictyear & "_" & dbpoparray(1)
                popval = dbpoparray(2)
                PopYearLookup.Add(dictkey, popval)
                dbline = zpr.ReadLine
                If dbline Is Nothing Then
                    Exit Do
                End If
                dbpoparray = Split(dbline, ",")
            Loop
        End If
        'economy
        If DBaseEco = True Then
            'clear existing gva lookup array
            EcoLookup.Clear()
            'loop through
            '***note that at present gva forecasts are only produced up to 2050
            Do
                dictyear = dbecoarray(0) - 2010
                dictkey = dictyear & "_" & dbecoarray(1)
                ecoval = dbecoarray(2)
                EcoYearLookup.Add(dictkey, ecoval)
                dbline = zer.ReadLine
                If dbline Is Nothing Then
                    Exit Do
                End If
                dbecoarray = Split(dbline, ",")
            Loop
        End If
        'energy
        '***need to add dictionary data for energy costs

        If NewRdLEV = True Then
            'generate road link EV file
            Call RoadLinkEVMain()
        End If
        If NewRdZEV = True Then
            'generate road zone EV file
            Call RoadZoneEVMain()
        End If
        If NewRlLEV = True Then
            'generate rail link EV file
            Call RailLinkEVMain()
        End If
        If NewRlZEV = True Then
            'generate rail zone EV file
            Call RlZoneEVMain()
        End If
        If NewAirEV = True Then
            'generate air EV file
            Call AirEVMain()
        End If
        If NewSeaEV = True Then
            'generate sea EV file
            Call SeaEVMain()
        End If

    End Sub

    Sub ModelElementLog()
        logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "The following modules were run:"
        Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        If RunRoadLink = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Road link"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If RunRoadZone = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Road zone"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If RunRailLink = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Rail link"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If RunRailZone = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Rail zone"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If RunAir = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Air"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
        If RunSea = True Then
            logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = "Sea"
            Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        End If
    End Sub

    Sub MissingValueError(ByVal ErrDict As String)
        Dim msg As String = "No " & ErrDict & " value found in lookup table for Zone " & OZone & " when updating input files.  Model run terminated."
        logarray(1, 0) = g_modelRunID : logarray(1, 1) = 1 : logarray(1, 2) = msg
        Call WriteData("Logfile", "", logarray, , , , g_LogVTypes)
        ErrorLog(ErrorSeverity.FATAL, "Missing Value Error", ErrDict, msg)
        Throw New System.Exception(msg)
    End Sub

    Sub WriteCrossSectorOutput()
        Dim cmd As New Odbc.OdbcCommand
        Dim m_conn As Odbc.OdbcConnection
        Dim theSQL As String = ""
        Dim m_sConnString As String

        'write separate capacity margin
        capacityMargin(1, 0) = g_modelRunID
        capacityMargin(1, 1) = g_modelRunYear
        For i = 2 To 4
            If capacityMargin(1, i) Is Nothing Then capacityMargin(1, i) = 0
        Next
        'Total Capacity Margin = 1 - (0.91 * Road Link CU + 0.08 * Rail Link CU + 0.01 * Air CU)
        capacityMargin(1, 5) = 1 - (0.91 * capacityMargin(1, 2) + 0.08 * capacityMargin(1, 3) + 0.01 * capacityMargin(1, 4))
        Call WriteData("CrossSector", "CapacityMargin", capacityMargin)


        'write cross sector output
        crossSectorArray(1, 0) = g_modelRunID
        crossSectorArray(1, 1) = g_modelRunYear
        For i = 2 To 5
            If crossSectorArray(1, i) Is Nothing Then crossSectorArray(1, i) = 0
        Next
        'get the free capacity as the capacity margin of the year
        crossSectorArray(1, 2) = capacityMargin(1, 5)
        Call WriteData("CrossSector", "CrossSector", crossSectorArray)


        ReDim crossSectorArray(1, 5)
        ReDim capacityMargin(1, 5)

        'update accumulated investment
        theSQL = "UPDATE " & Chr(34) & "TR_O_CrossSector" & Chr(34) & " SET accumulated_investment = accumulated_investment_data.accumulated_investment FROM (SELECT modelrun_id, year, investment, sum(investment) OVER (PARTITION BY modelrun_id ORDER BY year) as accumulated_investment FROM " & Chr(34) & "TR_O_CrossSector" & Chr(34) & " WHERE modelrun_id = " & g_modelRunID & ") as accumulated_investment_data WHERE " & Chr(34) & "TR_O_CrossSector" & Chr(34) & ".modelrun_id = accumulated_investment_data.modelrun_id AND " & Chr(34) & "TR_O_CrossSector" & Chr(34) & ".year = accumulated_investment_data.year"
        'theSQL = "UPDATE " & Chr(34) & "TR_O_CrossSector" & Chr(34) & " SET accumulated_investment = accumulated_investment_data.accumulated_investment FROM (SELECT modelrun_id, year, investment, sum(investment) OVER (PARTITION BY modelrun_id ORDER BY year) as accumulated_investment FROM " & Chr(34) & "TR_O_CrossSector" & Chr(34) & " WHERE modelrun_id = 285) as accumulated_investment_data WHERE " & Chr(34) & "TR_O_CrossSector" & Chr(34) & ".modelrun_id = accumulated_investment_data.modelrun_id AND " & Chr(34) & "TR_O_CrossSector" & Chr(34) & ".year = accumulated_investment_data.year"

        'If there is no connection to the database then establish one
        If m_conn Is Nothing Then
            m_sConnString = g_dbase
            m_conn = New Odbc.OdbcConnection(m_sConnString)
            m_conn.ConnectionTimeout = 60
            m_conn.Open()
        End If

        cmd.Connection = m_conn
        cmd.CommandText = theSQL
        cmd.ExecuteNonQuery()
    End Sub
    Sub CapChangeComb()
        Dim i As Integer
        Dim capNo As Integer = 1
        Dim capGroup() As String
        'read from capacity change combination lookup table
        'the capChangeArray is: 0 - id, 1 - CapChangeID, 2 - 11 - capacity_group1 to capacity_group10
        Call ReadData("CapChangeID", "", capChangeArray, , CapChangeID)

        'split the capacity group numbers
        capGroup = Split(capChangeArray(1, 3), ",")
        Array.Sort(capGroup)

        'if no additional cap group then exit
        If capGroup(0) < 1 Then Exit Sub

        'read the capacity group that should be considered by the model into the array
        For i = 1 To capGroup.Max
            If i = capGroup(capNo - 1) Then
                ReDim Preserve capGroupArray(capNo - 1)

                capGroupArray(capNo - 1) = i
                capNo += 1
            End If
        Next

        'ReDim Preserve capGroupArray(capNo)


    End Sub

    Protected Overrides Sub Finalize()
        MyBase.Finalize()
        Me.Dispose()
    End Sub

#Region "IDisposable Support"
    Private disposedValue As Boolean ' To detect redundant calls

    ' IDisposable
    Protected Overridable Sub Dispose(disposing As Boolean)
        If Not Me.disposedValue Then
            If disposing Then
                ' TODO: dispose managed state (managed objects).
            End If

            ' TODO: free unmanaged resources (unmanaged objects) and override Finalize() below.
            ' TODO: set large fields to null.
        End If
        Me.disposedValue = True
    End Sub

    ' TODO: override Finalize() only if Dispose(ByVal disposing As Boolean) above has code to free unmanaged resources.
    'Protected Overrides Sub Finalize()
    '    ' Do not change this code.  Put cleanup code in Dispose(ByVal disposing As Boolean) above.
    '    Dispose(False)
    '    MyBase.Finalize()
    'End Sub

    ' This code added by Visual Basic to correctly implement the disposable pattern.
    Public Sub Dispose() Implements IDisposable.Dispose
        ' Do not change this code.  Put cleanup code in Dispose(disposing As Boolean) above.
        Dispose(True)
        GC.SuppressFinalize(Me)
    End Sub
#End Region
End Class
