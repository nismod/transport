Module IntraZoneRailModel1pt1
    'this version can cope with the addition of new stations based on external demand forecasts 
    'it is dependent on module FullCDAM for file paths
    'it now includes a car fuel cost variable
    'it now also includes variable elasticities specified via an input file
    'it now also includdes a GJT variable
    'it also now includes the option to include variable form elasticities determined by the model
    'now also includes variable trip rate option
    'v1.6 now calculated by annual timesteps
    'values from previous year are stored in temp file and read at the start of the calculation for each year

    Dim RailZoneInputData As IO.FileStream
    Dim rirlz As IO.StreamReader
    Dim RailZoneExtInput As IO.FileStream
    Dim evrlz As IO.StreamReader
    Dim RailZoneOutputData As IO.FileStream
    Dim rorlz As IO.StreamWriter
    Dim RailZoneElasticities As IO.FileStream
    Dim rerlz As IO.StreamReader
    Dim srlz As IO.StreamReader
    Dim RlZOutputRow As String
    Dim RlZInput As String
    Dim RlZDetails() As String
    Dim RlZID(144, 0) As Long
    Dim FareE(144, 0) As Integer
    Dim RlZTripsS(144, 0) As Double
    Dim RlZPop(144, 0) As Double
    Dim RlZGva(144, 0) As Double
    Dim RlZCost(144, 0) As Double
    Dim RlZStat(144, 0) As Long
    Dim RlZCarFuel(144, 0) As Double
    Dim RlZExtVar(7, 144) As Double
    Dim YearCount As Integer
    Dim NewTripsS As Double
    Dim NewTripTotal As Double
    Dim RlZoneEl(11, 90) As String
    Dim RlZGJT(144, 0) As Double
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim RlzTripRates(90) As Double
    Dim InputCount As Long
    Dim RlZFile As IO.FileStream
    Dim rlzr As IO.StreamReader
    Dim rlzw As IO.StreamWriter
    Dim OutputRow As String
    Dim RlZLine As String
    Dim RlZArray() As String


    Public Sub RailZoneMain()

        'get the input files
        Call RlZSetFiles()

        'read in the elasticies
        Call ReadRlZElasticities()

        YearCount = 1
        Do Until YearCount > 90

            'get external variable values
            Call GetRlZExtVar()

            'read previous year value from temp file if not year 1
            Call ReadRlZInput()

            InputCount = 1

            Do Until InputCount > 144
                'update the input variables
                Call LoadRlZInput()

                'apply zone equation to adjust demand per station, and to get new total demand
                Call RailZoneTrips()

                'write output line
                Call RailZoneOutput()

                'update base values
                'Call RlZNewBaseValues()

                'write to temp file
                Call WriteRlZUpdate()

                InputCount += 1
            Loop

            'close the temp file
            rlzw.Close()

            YearCount += 1
        Loop
        'loop through all the zones in the input file

        'Close input and output files
        rirlz.Close()
        evrlz.Close()
        rorlz.Close()
        rerlz.Close()

        rlzr.Close()
        'delete the temp file
        System.IO.File.Delete(DirPath & FilePrefix & "RlZones.csv")

    End Sub

    Sub RlZSetFiles()
        Dim row As String
        Dim stratarray() As String
        'This sub selects the input data files

        RailZoneInputData = New IO.FileStream(DirPath & "RailZoneInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rirlz = New IO.StreamReader(RailZoneInputData, System.Text.Encoding.Default)
        'read header row
        row = rirlz.ReadLine

        If UpdateExtVars = True Then
            If NewRlZCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If
        RailZoneExtInput = New IO.FileStream(DirPath & EVFilePrefix & "RailZoneExtVar" & EVFileSuffix & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        evrlz = New IO.StreamReader(RailZoneExtInput, System.Text.Encoding.Default)
        'read header row
        row = evrlz.ReadLine

        RailZoneOutputData = New IO.FileStream(DirPath & FilePrefix & "RailZoneOutput.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rorlz = New IO.StreamWriter(RailZoneOutputData, System.Text.Encoding.Default)
        'write header row
        RlZOutputRow = "Yeary,ZoneID,TripsStaty,Stationsy,Tripsy"
        rorlz.WriteLine(RlZOutputRow)

        RailZoneElasticities = New IO.FileStream(DirPath & "Elasticity Files\TR" & Strategy & "\RailZoneElasticities.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rerlz = New IO.StreamReader(RailZoneElasticities, System.Text.Encoding.Default)
        'read header row
        row = rerlz.ReadLine

        If TripRates = "Strategy" Then
            StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
            srlz = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
            'read header row
            row = srlz.ReadLine
            For r = 1 To 90
                row = srlz.ReadLine
                stratarray = Split(row, ",")
                RlzTripRates(r) = stratarray(93)
            Next
            srlz.Close()
        End If

    End Sub

    Sub LoadRlZInput()

        If YearCount = 1 Then
            'read the input data for the zone
            RlZInput = rirlz.ReadLine

            RlZDetails = Split(RlZInput, ",")
            RlZID(InputCount, 0) = RlZDetails(0)
            FareE(InputCount, 0) = RlZDetails(2)
            RlZTripsS(InputCount, 0) = RlZDetails(3)
            RlZPop(InputCount, 0) = RlZDetails(4)
            RlZGva(InputCount, 0) = RlZDetails(5)
            RlZCost(InputCount, 0) = RlZDetails(6)
            RlZStat(InputCount, 0) = RlZDetails(7)
            RlZCarFuel(InputCount, 0) = RlZDetails(8)
            RlZGJT(InputCount, 0) = RlZDetails(9)

        End If

    End Sub

    Sub GetRlZExtVar()
        Dim rownum As Long
        Dim row As String
        Dim ExtVarRow() As String
        Dim r As Byte

        rownum = 1
        Do While rownum < 145
            'loop through 90 rows in the external variables file, storing the values in the external variable values array
            row = evrlz.ReadLine
            ExtVarRow = Split(row, ",")
            For r = 2 To 6
                RlZExtVar(r, rownum) = ExtVarRow(r)
            Next
            'the 7th value in the input data is the new trips at new stations, so we skip this, and assign the 8th value (GJT) to the 7th value in the array
            RlZExtVar(7, rownum) = ExtVarRow(8)
            rownum += 1
        Loop
    End Sub
    Sub ReadRlZElasticities()
        Dim row As String
        Dim elstring() As String
        Dim yearcheck As Integer
        Dim elcount As Integer

        yearcheck = 1

        Do
            'read in row from elasticities file
            row = rerlz.ReadLine
            If row Is Nothing Then
                Exit Do
            End If
            'split it into array - 1 is pop, 2 is gva, 3 is LondonFares, 4 is SEFares, 5 is PTEFares, 6 is OtherFares, 7 is carfuel, 8 is LondonGJT, 9 is SEGJT, 10 is PTEGJT, 11 iS OtherGJT
            elstring = Split(row, ",")
            elcount = 1
            Do While elcount < 12
                RlZoneEl(elcount, yearcheck) = elstring(elcount)
                elcount += 1
            Loop
            yearcheck += 1
        Loop
    End Sub

    Sub RailZoneTrips()
        Dim PopRat, GVARat, FarRat, TrpRat, CFuelRat, GJTRat As Double
        Dim FarEl, GJTEl As Double
        Dim NewStatCount As Integer

        'Select the appropriate fare elasticity based on the FareE value
        Select Case FareE(InputCount, 0)
            Case 1
                FarEl = RlZoneEl(3, YearCount)
                GJTEl = RlZoneEl(8, YearCount)
            Case 2
                FarEl = RlZoneEl(4, YearCount)
                GJTEl = RlZoneEl(9, YearCount)
            Case 3
                FarEl = RlZoneEl(5, YearCount)
                GJTEl = RlZoneEl(10, YearCount)
            Case 4
                FarEl = RlZoneEl(6, YearCount)
                GJTEl = RlZoneEl(11, YearCount)
            Case Else
                '****otherwise stop the model and write an error to the log file
        End Select

        'Calculate the values of the various input ratios
        'now includes option to use variable elasticities
        If VariableEl = True Then
            OldX = RlZTripsS(InputCount, 0)
            'pop ratio
            OldY = RlZPop(InputCount, 0)
            If TripRates = "Strategy" Then
                NewY = RlZExtVar(2, InputCount) * RlzTripRates(YearCount)
            Else
                NewY = RlZExtVar(2, InputCount)
            End If
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(1, YearCount)
                Call VarElCalc()
                PopRat = VarRat
            Else
                If TripRates = "Strategy" Then
                    PopRat = ((RlZExtVar(2, InputCount) * RlzTripRates(YearCount)) / RlZPop(InputCount, 0)) ^ RlZoneEl(1, YearCount)
                Else
                    PopRat = (RlZExtVar(2, InputCount) / RlZPop(InputCount, 0)) ^ RlZoneEl(1, YearCount)
                End If

            End If
            'gva ratio
            OldY = RlZGva(InputCount, 0)
            NewY = RlZExtVar(3, InputCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(2, YearCount)
                Call VarElCalc()
                GVARat = VarRat
            Else
                GVARat = (RlZExtVar(3, InputCount) / RlZGva(InputCount, 0)) ^ RlZoneEl(2, YearCount)
            End If
            'fare ratio
            OldY = RlZCost(InputCount, 0)
            NewY = RlZExtVar(4, InputCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = FarEl
                Call VarElCalc()
                FarRat = VarRat
            Else
                FarRat = (RlZExtVar(4, InputCount) / RlZCost(InputCount, 0)) ^ FarEl
            End If
            'car fuel ratio
            OldY = RlZCarFuel(InputCount, 0)
            NewY = RlZExtVar(6, InputCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(7, YearCount)
                Call VarElCalc()
                CFuelRat = VarRat
            Else
                CFuelRat = (RlZExtVar(6, InputCount) / RlZCarFuel(InputCount, 0)) ^ RlZoneEl(7, YearCount)
            End If
            'GJT ratio
            OldY = RlZGJT(InputCount, 0)
            NewY = RlZExtVar(7, InputCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = GJTEl
                Call VarElCalc()
                GJTRat = VarRat
            Else
                GJTRat = (RlZExtVar(7, InputCount) / RlZGJT(InputCount, 0)) ^ GJTEl
            End If
        Else
            If TripRates = "Strategy" Then
                PopRat = ((RlZExtVar(2, InputCount) * RlzTripRates(YearCount)) / RlZPop(InputCount, 0)) ^ RlZoneEl(1, YearCount)
            Else
                PopRat = (RlZExtVar(2, InputCount) / RlZPop(InputCount, 0)) ^ RlZoneEl(1, YearCount)
            End If
            GVARat = (RlZExtVar(3, InputCount) / RlZGva(InputCount, 0)) ^ RlZoneEl(2, YearCount)
            FarRat = (RlZExtVar(4, InputCount) / RlZCost(InputCount, 0)) ^ FarEl
            CFuelRat = (RlZExtVar(6, InputCount) / RlZCarFuel(InputCount, 0)) ^ RlZoneEl(7, YearCount)
            GJTRat = (RlZExtVar(7, InputCount) / RlZGJT(InputCount, 0)) ^ GJTEl
        End If

        'Combine these ratios to get the trip ratio
        TrpRat = PopRat * GVARat * FarRat * CFuelRat * GJTRat

        'Multiply the trip ratio by the previous year's trips per station to get the new trips per station figure
        NewTripsS = RlZTripsS(InputCount, 0) * TrpRat

        'check if new stations have been added
        If RlZExtVar(5, InputCount) > RlZStat(InputCount, 0) Then
            'if so then calculate how many
            NewStatCount = RlZExtVar(5, InputCount) - RlZStat(InputCount, 0)
            'calculate the total number of trips in this situation
            NewTripTotal = (NewTripsS * RlZStat(InputCount, 0)) + (RlZExtVar(7, InputCount) * NewStatCount)
            'recalculate the number of trips per station
            NewTripsS = NewTripTotal / (RlZStat(InputCount, 0) + NewStatCount)
        Else
            'otherwise just multiply new trips per station by number of stations to get total trips
            NewTripTotal = NewTripsS * RlZExtVar(5, InputCount)
        End If

    End Sub

    Sub VarElCalc()
        Dim alpha, beta As Double
        Dim xnew As Double

        alpha = OldX / Math.Exp(OldEl)
        beta = (Math.Log(OldX / alpha)) / OldY
        xnew = alpha * Math.Exp(beta * NewY)
        VarRat = xnew / OldX

    End Sub

    Sub RailZoneOutput()
        'combine output values into output string
        RlZOutputRow = YearCount & "," & RlZID(InputCount, 0) & "," & NewTripsS & "," & RlZExtVar(5, InputCount) & "," & NewTripTotal

        'write output string to file
        rorlz.WriteLine(RlZOutputRow)
    End Sub

    Sub RlZNewBaseValues()
        'set base values to equal the values from the current year
        RlZTripsS(InputCount, 0) = NewTripsS
        RlZPop(InputCount, 0) = RlZExtVar(2, InputCount)
        RlZGva(InputCount, 0) = RlZExtVar(3, InputCount)
        RlZCost(InputCount, 0) = RlZExtVar(4, InputCount)
        RlZStat(InputCount, 0) = RlZExtVar(5, InputCount)
        RlZCarFuel(InputCount, 0) = RlZExtVar(6, InputCount)
        RlZGJT(InputCount, 0) = RlZExtVar(7, InputCount)
    End Sub

    Sub ReadRlZInput()

        If YearCount = 1 Then
            'year 1 will use the initial input file
        Else
            'read the temp file "Flows.csv"
            RlZFile = New IO.FileStream(DirPath & FilePrefix & "RlZones.csv", IO.FileMode.Open, IO.FileAccess.Read)
            rlzr = New IO.StreamReader(RlZFile, System.Text.Encoding.Default)
            'read header line
            rlzr.ReadLine()

            'read temp file for each link
            InputCount = 1

            Do While InputCount < 145

                RlZLine = rlzr.ReadLine
                RlZArray = Split(RlZLine, ",")

                RlZID(InputCount, 0) = RlZArray(1)
                RlZPop(InputCount, 0) = RlZArray(2)
                RlZGva(InputCount, 0) = RlZArray(3)
                RlZCost(InputCount, 0) = RlZArray(4)
                RlZStat(InputCount, 0) = RlZArray(5)
                RlZCarFuel(InputCount, 0) = RlZArray(6)
                RlZGJT(InputCount, 0) = RlZArray(7)
                RlZTripsS(InputCount, 0) = RlZArray(8)
                FareE(InputCount, 0) = RlZArray(9)

                InputCount += 1
            Loop

            rlzr.Close()
            'delete the temp file to recreate for current year
            System.IO.File.Delete(DirPath & FilePrefix & "RlZones.csv")

        End If

        'create a temp file "Flows.csv"
        RlZFile = New IO.FileStream(DirPath & FilePrefix & "RlZones.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rlzw = New IO.StreamWriter(RlZFile, System.Text.Encoding.Default)

        'write header row

        OutputRow = "Yeary,FlowID,PopZ1Base,PopZ2Base,GVAZ1Base,GVAZ2Base,OldDelays,RlLinkCost,CarFuel,OldTrains,OldTracks,MaxTDBase,CUOld,CUNew,"
        rlzw.WriteLine(OutputRow)

    End Sub

    Sub WriteRlZUpdate()

        'set base values to equal the values from the current year
        RlZPop(InputCount, 0) = RlZExtVar(2, InputCount)
        RlZGva(InputCount, 0) = RlZExtVar(3, InputCount)
        RlZCost(InputCount, 0) = RlZExtVar(4, InputCount)
        RlZStat(InputCount, 0) = RlZExtVar(5, InputCount)
        RlZCarFuel(InputCount, 0) = RlZExtVar(6, InputCount)
        RlZGJT(InputCount, 0) = RlZExtVar(7, InputCount)
        RlZTripsS(InputCount, 0) = NewTripsS

        'write second row
        OutputRow = YearCount & "," & RlZID(InputCount, 0) & "," & RlZPop(InputCount, 0) & "," & RlZGva(InputCount, 0) & "," & RlZCost(InputCount, 0) & "," & RlZStat(InputCount, 0) & "," & RlZCarFuel(InputCount, 0) & "," & RlZGJT(InputCount, 0) & "," & RlZTripsS(InputCount, 0) & "," & FareE(InputCount, 0) & ","

        rlzw.WriteLine(OutputRow)

    End Sub

End Module
