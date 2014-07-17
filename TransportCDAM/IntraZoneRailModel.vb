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
    'v1.7 now corporate with Database function, read/write are using the function in database interface
    Dim RailZoneExtInput As IO.FileStream
    Dim evrlz As IO.StreamReader
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
    Dim RlZExtVar(144, 8) As String
    Dim YearCount As Integer
    Dim NewTripsS As Double
    Dim NewTripTotal As Double
    Dim RlZoneEl(90, 11) As String
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
    Dim InputArray(144, 9) As String
    Dim OutputArray(144, 4) As String
    Dim TempArray(144, 9) As String
    Dim stratarray(90, 95) As String


    Public Sub RailZoneMain()

        'get the input files
        Call RlZSetFiles()

        'initialise year count
        YearCount = StartYear

        Do Until YearCount > StartYear + Duration

            'get external variable values
            Call ReadData("RailZone", "ExtVar", RlZExtVar, , YearCount)

            'read from initial file if year 1, otherwise update from temp file
            If YearCount = 1 Then
                Call ReadData("RailZone", "Input", InputArray, True)
            Else
                Call ReadData("RailZone", "Input", InputArray, False)
            End If

            'loop through all zones
            InputCount = 1

            Do Until InputCount > 144

                'update the input variables base on inputarray
                Call LoadRlZInput()

                'apply zone equation to adjust demand per station, and to get new total demand
                Call RailZoneTrips()

                'write output line to outputarray and temp array
                Call RailZoneOutput()

                InputCount += 1
            Loop

            'create file is true if it is the initial year and write to outputfile and temp file
            If YearCount = StartYear Then
                Call WriteData("RailZone", "Output", OutputArray, TempArray, True)
            Else
                Call WriteData("RailZone", "Output", OutputArray, TempArray, False)
            End If


            YearCount += 1
        Loop


    End Sub

    Sub RlZSetFiles()
        Dim row As String
        'This sub selects the input data files

        If UpdateExtVars = True Then
            If NewRlZCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If

        'read in the elasticies
        Call ReadData("RailZone", "Elasticity", RlZoneEl)

        'read in the strategy
        If TripRates = "Strategy" Then
            Call ReadData("Strategy", "", stratarray)
            For r = 1 To 90
                RlzTripRates(r) = stratarray(r, 93)
            Next
        End If

    End Sub

    Sub LoadRlZInput()

        'read the input data for the inputarray which is from the database
        If YearCount = 1 Then
            'if it is initial year, read from the initial input
            RlZID(InputCount, 0) = InputArray(InputCount, 0)
            FareE(InputCount, 0) = InputArray(InputCount, 2)
            RlZTripsS(InputCount, 0) = InputArray(InputCount, 3)
            RlZPop(InputCount, 0) = InputArray(InputCount, 4)
            RlZGva(InputCount, 0) = InputArray(InputCount, 5)
            RlZCost(InputCount, 0) = InputArray(InputCount, 6)
            RlZStat(InputCount, 0) = InputArray(InputCount, 7)
            RlZCarFuel(InputCount, 0) = InputArray(InputCount, 8)
            RlZGJT(InputCount, 0) = InputArray(InputCount, 9)
        Else
            'if not year 1, read from the Input file
            RlZID(InputCount, 0) = InputArray(InputCount, 1)
            RlZPop(InputCount, 0) = InputArray(InputCount, 2)
            RlZGva(InputCount, 0) = InputArray(InputCount, 3)
            RlZCost(InputCount, 0) = InputArray(InputCount, 4)
            RlZStat(InputCount, 0) = InputArray(InputCount, 5)
            RlZCarFuel(InputCount, 0) = InputArray(InputCount, 6)
            RlZGJT(InputCount, 0) = InputArray(InputCount, 7)
            RlZTripsS(InputCount, 0) = InputArray(InputCount, 8)
            FareE(InputCount, 0) = InputArray(InputCount, 9)

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
                FarEl = RlZoneEl(YearCount, 3)
                GJTEl = RlZoneEl(YearCount, 8)
            Case 2
                FarEl = RlZoneEl(YearCount, 4)
                GJTEl = RlZoneEl(YearCount, 9)
            Case 3
                FarEl = RlZoneEl(YearCount, 5)
                GJTEl = RlZoneEl(YearCount, 10)
            Case 4
                FarEl = RlZoneEl(YearCount, 6)
                GJTEl = RlZoneEl(YearCount, 11)
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
                NewY = RlZExtVar(InputCount, 2) * RlzTripRates(YearCount)
            Else
                NewY = RlZExtVar(InputCount, 2)
            End If
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(YearCount, 1)
                Call VarElCalc()
                PopRat = VarRat
            Else
                If TripRates = "Strategy" Then
                    PopRat = ((RlZExtVar(InputCount, 2) * RlzTripRates(YearCount)) / RlZPop(InputCount, 0)) ^ RlZoneEl(YearCount, 1)
                Else
                    PopRat = (RlZExtVar(InputCount, 2) / RlZPop(InputCount, 0)) ^ RlZoneEl(YearCount, 1)
                End If

            End If
            'gva ratio
            OldY = RlZGva(InputCount, 0)
            NewY = RlZExtVar(InputCount, 3)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(YearCount, 2)
                Call VarElCalc()
                GVARat = VarRat
            Else
                GVARat = (RlZExtVar(InputCount, 3) / RlZGva(InputCount, 0)) ^ RlZoneEl(YearCount, 2)
            End If
            'fare ratio
            OldY = RlZCost(InputCount, 0)
            NewY = RlZExtVar(InputCount, 4)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = FarEl
                Call VarElCalc()
                FarRat = VarRat
            Else
                FarRat = (RlZExtVar(InputCount, 4) / RlZCost(InputCount, 0)) ^ FarEl
            End If
            'car fuel ratio
            OldY = RlZCarFuel(InputCount, 0)
            NewY = RlZExtVar(InputCount, 6)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(YearCount, 7)
                Call VarElCalc()
                CFuelRat = VarRat
            Else
                CFuelRat = (RlZExtVar(InputCount, 6) / RlZCarFuel(InputCount, 0)) ^ RlZoneEl(YearCount, 7)
            End If
            'GJT ratio
            OldY = RlZGJT(InputCount, 0)
            NewY = RlZExtVar(InputCount, 8)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = GJTEl
                Call VarElCalc()
                GJTRat = VarRat
            Else
                GJTRat = (RlZExtVar(InputCount, 8) / RlZGJT(InputCount, 0)) ^ GJTEl
            End If
        Else
            If TripRates = "Strategy" Then
                PopRat = ((RlZExtVar(InputCount, 2) * RlzTripRates(YearCount)) / RlZPop(InputCount, 0)) ^ RlZoneEl(YearCount, 1)
            Else
                PopRat = (RlZExtVar(InputCount, 2) / RlZPop(InputCount, 0)) ^ RlZoneEl(YearCount, 1)
            End If
            GVARat = (RlZExtVar(InputCount, 3) / RlZGva(InputCount, 0)) ^ RlZoneEl(YearCount, 2)
            FarRat = (RlZExtVar(InputCount, 4) / RlZCost(InputCount, 0)) ^ FarEl
            CFuelRat = (RlZExtVar(InputCount, 6) / RlZCarFuel(InputCount, 0)) ^ RlZoneEl(YearCount, 7)
            GJTRat = (RlZExtVar(InputCount, 7) / RlZGJT(InputCount, 0)) ^ GJTEl
        End If

        'Combine these ratios to get the trip ratio
        TrpRat = PopRat * GVARat * FarRat * CFuelRat * GJTRat

        'Multiply the trip ratio by the previous year's trips per station to get the new trips per station figure
        NewTripsS = RlZTripsS(InputCount, 0) * TrpRat

        'check if new stations have been added
        If RlZExtVar(InputCount, 5) > RlZStat(InputCount, 0) Then
            'if so then calculate how many
            NewStatCount = RlZExtVar(InputCount, 5) - RlZStat(InputCount, 0)
            'calculate the total number of trips in this situation
            NewTripTotal = (NewTripsS * RlZStat(InputCount, 0)) + (RlZExtVar(InputCount, 8) * NewStatCount)
            'recalculate the number of trips per station
            NewTripsS = NewTripTotal / (RlZStat(InputCount, 0) + NewStatCount)
        Else
            'otherwise just multiply new trips per station by number of stations to get total trips
            NewTripTotal = NewTripsS * RlZExtVar(InputCount, 5)
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
        'combine output values into output array
        OutputArray(InputCount, 0) = YearCount
        OutputArray(InputCount, 1) = RlZID(InputCount, 0)
        OutputArray(InputCount, 2) = NewTripsS
        OutputArray(InputCount, 3) = RlZExtVar(InputCount, 5)
        OutputArray(InputCount, 4) = NewTripTotal

        'update the variables
        RlZTripsS(InputCount, 0) = NewTripsS
        RlZPop(InputCount, 0) = RlZExtVar(InputCount, 2)
        RlZGva(InputCount, 0) = RlZExtVar(InputCount, 3)
        RlZCost(InputCount, 0) = RlZExtVar(InputCount, 4)
        RlZStat(InputCount, 0) = RlZExtVar(InputCount, 5)
        RlZCarFuel(InputCount, 0) = RlZExtVar(InputCount, 6)
        RlZGJT(InputCount, 0) = RlZExtVar(InputCount, 8)

        'write to the temp file
        TempArray(InputCount, 0) = YearCount
        TempArray(InputCount, 1) = RlZID(InputCount, 0)
        TempArray(InputCount, 2) = RlZPop(InputCount, 0)
        TempArray(InputCount, 3) = RlZGva(InputCount, 0)
        TempArray(InputCount, 4) = RlZCost(InputCount, 0)
        TempArray(InputCount, 5) = RlZStat(InputCount, 0)
        TempArray(InputCount, 6) = RlZCarFuel(InputCount, 0)
        TempArray(InputCount, 7) = RlZGJT(InputCount, 0)
        TempArray(InputCount, 8) = RlZTripsS(InputCount, 0)
        TempArray(InputCount, 9) = FareE(InputCount, 0)

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
                'TempArray = Split(RlZLine, ",")

                RlZID(InputCount, 0) = TempArray(InputCOunt, 1)
                RlZPop(InputCount, 0) = TempArray(InputCOunt, 2)
                RlZGva(InputCount, 0) = TempArray(InputCOunt, 3)
                RlZCost(InputCount, 0) = TempArray(InputCOunt, 4)
                RlZStat(InputCount, 0) = TempArray(InputCOunt, 5)
                RlZCarFuel(InputCount, 0) = TempArray(InputCOunt, 6)
                RlZGJT(InputCount, 0) = TempArray(InputCOunt, 7)
                RlZTripsS(InputCount, 0) = TempArray(InputCOunt, 8)
                FareE(InputCount, 0) = TempArray(InputCOunt, 9)

                InputCount += 1
            Loop

            rlzr.Close()
            'delete the temp file to recreate for current year
            System.IO.File.Delete(DirPath & FilePrefix & "RlZones.csv")

        End If

        'create a temp file 
        RlZFile = New IO.FileStream(DirPath & FilePrefix & "RlZones.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rlzw = New IO.StreamWriter(RlZFile, System.Text.Encoding.Default)

        'write header row

        OutputRow = "Yeary,ZoneID,PopZ,GvaZ,Cost,Stations,CarFuel,GJT,TripsStat,FareE,"
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
