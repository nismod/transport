﻿Public Class FullCDAM
    'this version incorporates EV input from the database.

    Public Function runCDAM(ByVal ModelRun_ID As Integer, ByVal Model_Year As Integer, ByVal Dbase As String) As Boolean
        'Try

        'Store global variables
        g_modelRunID = ModelRun_ID
        g_modelRunYear = Model_Year
        g_dbase = Dbase

        'Get Model Run Details including 
        If getModelRunDetails() = False Then
            Throw New System.Exception("Error getting Model Run details from database")
        End If

        'Get Model run parameters
        GetParameters()

        'Run Transport Model
        FullMain()

        Return True

        'Catch ex As Exception
        'Throw ex
        'Return False
        'End Try
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

        'TODO - Would be nice to replace this with some EXECUTE function that assigns these variables dynamically
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
                    ElCritValue = CDbl(ary(i, 5))
                Case "CongestionCharge"
                    CongestionCharge = CBool(ary(i, 5))
                Case "ConChargeYear"
                    ConChargeYear = CInt(ary(i, 5))
                Case "ConChargePer"
                    ConChargePer = CInt(ary(i, 5))
                Case "CarbonCharge"
                    CarbonCharge = CBool(ary(i, 5))
                Case "CarbChargeYear"
                    CarbChargeYear = CInt(ary(i, 5))
                Case "WPPL"
                    WPPL = CBool(ary(i, 5))
                Case "WPPLYear"
                    WPPLYear = CInt(ary(i, 5))
                Case "WPPLPer"
                    WPPLPer = CInt(ary(i, 5))
                Case "RailCCharge"
                    RailCCharge = CBool(ary(i, 5))
                Case "RlCChargeYear"
                    RlCChargeYear = CInt(ary(i, 5))
                Case "RailChargePer"
                    RailChargePer = CInt(ary(i, 5))
                Case "RlCaCharge"
                    RlCaCharge = CBool(ary(i, 5))
                Case "RlCaChYear"
                    RlCaChYear = CInt(ary(i, 5))
                Case "AirCCharge"
                    AirCCharge = CBool(ary(i, 5))
                Case "AirChargeYear"
                    AirChargeYear = CInt(ary(i, 5))
                Case "AirChargePer"
                    AirChargePer = CInt(ary(i, 5))
                Case "AirCaCharge"
                    AirCaCharge = CBool(ary(i, 5))
                Case "AirCaChYear"
                    AirCaChYear = CInt(ary(i, 5))
                Case "SmarterChoices"
                    SmarterChoices = CBool(ary(i, 5))
                Case "SmartIntro"
                    SmartIntro = CInt(ary(i, 5))
                Case "SmartPer"
                    SmartPer = CInt(ary(i, 5))
                Case "SmartYears"
                    SmartYears = CInt(ary(i, 5))
                Case "SmartFrt"
                    SmartFrt = CBool(ary(i, 5))
                Case "SmFrtIntro"
                    SmFrtIntro = CInt(ary(i, 5))
                Case "SmFrtPer"
                    SmFrtPer = CInt(ary(i, 5))
                Case "SmFrtYears"
                    SmFrtYears = CInt(ary(i, 5))
                Case "UrbanFrt"
                    UrbanFrt = CBool(ary(i, 5))
                Case "UrbFrtIntro"
                    UrbFrtIntro = CInt(ary(i, 5))
                Case "UrbFrtPer"
                    UrbFrtPer = CInt(ary(i, 5))
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
                    NewRoadLanes = CInt(ary(i, 5))
                Case "NewRailTracks"
                    NewRailTracks = CInt(ary(i, 5))
                Case "NewRlZCap"
                    NewRlZCap = CBool(ary(i, 5))
                Case "NewAirRun"
                    NewAirRun = CInt(ary(i, 5))
                Case "NewAirTerm"
                    NewAirTerm = CInt(ary(i, 5))
                Case "NewSeaCap"
                    NewSeaCap = CBool(ary(i, 5))
                Case "NewSeaTonnes"
                    NewSeaTonnes = CInt(ary(i, 5))
                Case "RlElect"
                    RlElect = CBool(ary(i, 5))
                Case "ElectKmPerYear"
                    ElectKmPerYear = CInt(ary(i, 5))
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
                    RlPeakHeadway = CInt(ary(i, 5))
                Case "RdZSpdSource"
                    RdZSpdSource = CStr(ary(i, 5))
                Case "TripRates"
                    TripRates = CBool(ary(i, 5))
                Case Else
                    Stop
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
        'read fuel price for previous year (1,x) and current year (2,x)
        Call get_fuelprice_by_modelrun_id(g_modelRunID, g_modelRunYear, 0)
        If RlZEneSource = "Database" Then
            'If g_modelRunYear = 2010 Then y = 1 Else y = g_modelRunYear - g_initialYear + 1 'TODO - this needs fixing once we go to database for energy 
            InDieselOldAll = enearray(1, 2)
            InElectricOldAll = enearray(1, 3)
            InDieselNewAll = enearray(2, 2)
            InElectricNewAll = enearray(2, 3)
        End If

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
            End
        End If

        'add model elements to log file
        Call ModelElementLog()

        'if RoadLink model is selected then run that model
        If RunRoadLink = True Then
            'Call RoadLinkMain()
            logarray(logNum, 0) = "Road link model run completed"
            logNum += 1
        End If

        'if RoadZone model is selected then run that model
        If RunRoadZone = True Then
            'modification to allow use of old or new speed calculations
            If RdZSpdSource = "Elasticity" Then
                Call RoadZoneMain()
            Else
                Call RoadZoneMainNew()
            End If
            logarray(logNum, 0) = "Road zone model run completed"
            logNum += 1
        End If

        'if RailLink model is selected then run that model
        If RunRailLink = True Then
            Call RailLinkMain()
            logarray(logNum, 0) = "Rail link model run completed"
            logNum += 1
        End If

        'if RailZone model is selected then run that model
        If RunRailZone = True Then
            Call RailZoneMain()
            logarray(logNum, 0) = "Rail zone model run completed"
            logNum += 1
        End If

        'if Air model is selected then run that model
        If RunAir = True Then
            Call AirMain()
            logarray(logNum, 0) = "Air model run completed"
            logNum += 1
        End If

        'if Sea model is selected then run that model
        If RunSea = True Then
            Call SeaMain()
            logarray(logNum, 0) = "Sea model run completed"
            logNum += 1
        End If


        'Write closing lines of log file
        Call CloseLog()

    End Sub

    Sub CreateLog()
        'write header rows
        logarray(1, 0) = "ITRC Transport CDAM"
        logarray(2, 0) = "Model run commenced at " & System.DateTime.Now
        logNum = 3
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
        logarray(logNum, 0) = "The following external variable files were generated:"
        logNum += 1
        If NewRdLEV = True Then
            logarray(logNum, 0) = "Road link external variables"
            logNum += 1
        End If
        If NewRdZEV = True Then
            logarray(logNum, 0) = "Road zone external variables"
            logNum += 1
        End If
        If NewRlLEV = True Then
            logarray(logNum, 0) = "Rail link external variables"
            logNum += 1
        End If
        If NewRlZEV = True Then
            logarray(logNum, 0) = "Rail zone external variables"
            logNum += 1
        End If
        If NewAirEV = True Then
            logarray(logNum, 0) = "Airport external variables"
            logNum += 1
        End If
        If NewSeaEV = True Then
            'Call SeaEVMain()
            logarray(logNum, 0) = "Seaport external variables"
            logNum += 1
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
        logarray(logNum, 0) = "The following modules were run:"
        logNum += 1
        If RunRoadLink = True Then
            logarray(logNum, 0) = "Road link"
            logNum += 1
        End If
        If RunRoadZone = True Then
            logarray(logNum, 0) = "Road zone"
            logNum += 1
        End If
        If RunRailLink = True Then
            logarray(logNum, 0) = "Rail link"
            logNum += 1
        End If
        If RunRailZone = True Then
            logarray(logNum, 0) = "Rail zone"
            logNum += 1
        End If
        If RunAir = True Then
            logarray(logNum, 0) = "Air"
            logNum += 1
        End If
        If RunSea = True Then
            logarray(logNum, 0) = "Sea"
            logNum += 1
        End If
    End Sub

    Sub MissingValueError(ByVal ErrDict As String)
        logarray(logNum, 0) = "No " & ErrDict & " value found in lookup table for Zone " & OZone & " when updating input files.  Model run terminated."
        logNum += 1
        Call WriteData("Logfile", "", logarray)
        MsgBox("Model run failed.  Please consult the log file for details.")
        End
    End Sub
End Class
