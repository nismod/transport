Module IntraZoneRailModel1pt1
    'this version can cope with the addition of new stations based on external demand forecasts 
    'it is dependent on module FullCDAM for file paths
    'it now includes a car fuel cost variable
    'it now also includes variable elasticities specified via an input file
    'it now also includdes a GJT variable
    'it also now includes the option to include variable form elasticities determined by the model
    'now also includes variable trip rate option

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
    Dim RlZID As Long
    Dim FareE As Integer
    Dim RlZTripsS As Double
    Dim RlZPop As Double
    Dim RlZGva As Double
    Dim RlZCost As Double
    Dim RlZStat As Long
    Dim RlZCarFuel As Double
    Dim RlZExtVar(7, 90) As Double
    Dim YearCount As Integer
    Dim NewTripsS As Double
    Dim NewTripTotal As Double
    Dim RlZoneEl(11, 90) As String
    Dim RlZGJT As Double
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim RlzTripRates(90) As Double

    Public Sub RailZoneMain()

        'get the input files
        Call RlZSetFiles()

        'read in the elasticies
        Call ReadRlZElasticities()

        'loop through all the zones in the input file
        Do
            'read the input data for the zone
            RlZInput = rirlz.ReadLine

            'check if at end if file
            If RlZInput Is Nothing Then
                Exit Do
            Else
                'update the input variables
                Call LoadRlZInput()

                'get external variable values
                Call GetRlZExtVar()

                'set year counter to one
                YearCount = 1

                Do Until YearCount > 90
                    'apply zone equation to adjust demand per station, and to get new total demand
                    Call RailZoneTrips()

                    'write output line
                    Call RailZoneOutput()

                    'update base values
                    Call RlZNewBaseValues()

                    'move on to next year
                    YearCount += 1
                Loop

            End If
        Loop

        'Close input and output files
        rirlz.Close()
        evrlz.Close()
        rorlz.Close()
        rerlz.Close()

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
        RlZOutputRow = "ZoneID,Yeary,TripsStaty,Stationsy,Tripsy"
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
        RlZDetails = Split(RlZInput, ",")
        RlZID = RlZDetails(0)
        FareE = RlZDetails(2)
        RlZTripsS = RlZDetails(3)
        RlZPop = RlZDetails(4)
        RlZGva = RlZDetails(5)
        RlZCost = RlZDetails(6)
        RlZStat = RlZDetails(7)
        RlZCarFuel = RlZDetails(8)
        RlZGJT = RlZDetails(9)

    End Sub

    Sub GetRlZExtVar()
        Dim rownum As Long
        Dim row As String
        Dim ExtVarRow() As String
        Dim r As Byte
        Dim YearCount As Integer

        rownum = 1
        YearCount = 1
        Do While rownum < 91
            'loop through 90 rows in the external variables file, storing the values in the external variable values array
            row = evrlz.ReadLine
            ExtVarRow = Split(row, ",")
            If ExtVarRow(1) = YearCount Then
                'as long as the year counter corresponds to the year value in the input data, write values to the array
                For r = 1 To 6
                    RlZExtVar(r, rownum) = ExtVarRow(r)
                Next
                'the 7th value in the input data is the new trips at new stations, so we skip this, and assign the 8th value (GJT) to the 7th value in the array
                RlZExtVar(7, rownum) = ExtVarRow(8)
                rownum += 1
            Else
                '****otherwise stop the model and write an error to the log file
            End If
            YearCount += 1
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
        Select Case FareE
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
            OldX = RlZTripsS
            'pop ratio
            OldY = RlZPop
            If TripRates = "Strategy" Then
                NewY = RlZExtVar(2, YearCount) * RlzTripRates(YearCount)
            Else
                NewY = RlZExtVar(2, YearCount)
            End If
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(1, YearCount)
                Call VarElCalc()
                PopRat = VarRat
            Else
                If TripRates = "Strategy" Then
                    PopRat = ((RlZExtVar(2, YearCount) * RlzTripRates(YearCount)) / RlZPop) ^ RlZoneEl(1, YearCount)
                Else
                    PopRat = (RlZExtVar(2, YearCount) / RlZPop) ^ RlZoneEl(1, YearCount)
                End If

            End If
            'gva ratio
            OldY = RlZGva
            NewY = RlZExtVar(3, YearCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(2, YearCount)
                Call VarElCalc()
                GVARat = VarRat
            Else
                GVARat = (RlZExtVar(3, YearCount) / RlZGva) ^ RlZoneEl(2, YearCount)
            End If
            'fare ratio
            OldY = RlZCost
            NewY = RlZExtVar(4, YearCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = FarEl
                Call VarElCalc()
                FarRat = VarRat
            Else
                FarRat = (RlZExtVar(4, YearCount) / RlZCost) ^ FarEl
            End If
            'car fuel ratio
            OldY = RlZCarFuel
            NewY = RlZExtVar(6, YearCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(7, YearCount)
                Call VarElCalc()
                CFuelRat = VarRat
            Else
                CFuelRat = (RlZExtVar(6, YearCount) / RlZCarFuel) ^ RlZoneEl(7, YearCount)
            End If
            'GJT ratio
            OldY = RlZGJT
            NewY = RlZExtVar(7, YearCount)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = GJTEl
                Call VarElCalc()
                GJTRat = VarRat
            Else
                GJTRat = (RlZExtVar(7, YearCount) / RlZGJT) ^ GJTEl
            End If
        Else
            If TripRates = "Strategy" Then
                PopRat = ((RlZExtVar(2, YearCount) * RlzTripRates(YearCount)) / RlZPop) ^ RlZoneEl(1, YearCount)
            Else
                PopRat = (RlZExtVar(2, YearCount) / RlZPop) ^ RlZoneEl(1, YearCount)
            End If
            GVARat = (RlZExtVar(3, YearCount) / RlZGva) ^ RlZoneEl(2, YearCount)
            FarRat = (RlZExtVar(4, YearCount) / RlZCost) ^ FarEl
            CFuelRat = (RlZExtVar(6, YearCount) / RlZCarFuel) ^ RlZoneEl(7, YearCount)
            GJTRat = (RlZExtVar(7, YearCount) / RlZGJT) ^ GJTEl
        End If

        'Combine these ratios to get the trip ratio
        TrpRat = PopRat * GVARat * FarRat * CFuelRat * GJTRat

        'Multiply the trip ratio by the previous year's trips per station to get the new trips per station figure
        NewTripsS = RlZTripsS * TrpRat

        'check if new stations have been added
        If RlZExtVar(5, YearCount) > RlZStat Then
            'if so then calculate how many
            NewStatCount = RlZExtVar(5, YearCount) - RlZStat
            'calculate the total number of trips in this situation
            NewTripTotal = (NewTripsS * RlZStat) + (RlZExtVar(7, YearCount) * NewStatCount)
            'recalculate the number of trips per station
            NewTripsS = NewTripTotal / (RlZStat + NewStatCount)
        Else
            'otherwise just multiply new trips per station by number of stations to get total trips
            NewTripTotal = NewTripsS * RlZExtVar(5, YearCount)
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
        RlZOutputRow = RlZID & "," & YearCount & "," & NewTripsS & "," & RlZExtVar(5, YearCount) & "," & NewTripTotal

        'write output string to file
        rorlz.WriteLine(RlZOutputRow)
    End Sub

    Sub RlZNewBaseValues()
        'set base values to equal the values from the current year
        RlZTripsS = NewTripsS
        RlZPop = RlZExtVar(2, YearCount)
        RlZGva = RlZExtVar(3, YearCount)
        RlZCost = RlZExtVar(4, YearCount)
        RlZStat = RlZExtVar(5, YearCount)
        RlZCarFuel = RlZExtVar(6, YearCount)
        RlZGJT = RlZExtVar(7, YearCount)
    End Sub
End Module
