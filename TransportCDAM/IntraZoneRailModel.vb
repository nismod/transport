Module IntraZoneRailModel
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
    'now all file related functions are using databaseinterface
    '1.9 now the module can run with database connection and read/write from/to database

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
    Dim RlZExtVar(144, 11) As String
    Dim RlZPreExtVar(144, 11) As String
    Dim NewTripsS As Double
    Dim NewTripTotal As Double
    Dim RlZoneEl(90, 11) As String
    Dim RlZGJT(144, 0) As Double
    Dim OldY, OldX, OldEl, NewY As Double
    Dim VarRat As Double
    Dim RlzTripRates As Double
    Dim InputCount As Long
    Dim OutputRow As String
    Dim RlZLine As String
    Dim InputArray(144, 14) As String
    Dim OutputArray(145, 5) As String
    Dim TempArray(145, 4) As String


    Public Sub RailZoneMain()

        'read all related files
        Call RlZSetFiles()


        'get external variable values
        Call ReadData("RailZone", "ExtVar", RlZExtVar, g_modelRunYear)

        'get previous year external variable values as base value
        Call ReadData("RailZone", "ExtVar", RlZPreExtVar, g_modelRunYear - 1)

        'Input data
        Call ReadData("RailZone", "Input", InputArray, g_modelRunYear)

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
        'v1.9 now write to database
        If g_modelRunYear = g_initialYear Then
            Call WriteData("RailZone", "Output", OutputArray, , True)
            Call WriteData("RailZone", "Temp", TempArray, , True)
        Else
            Call WriteData("RailZone", "Output", OutputArray, , False)
            Call WriteData("RailZone", "Temp", TempArray, , False)
        End If




    End Sub

    Sub RlZSetFiles()
        'This sub selects the input data files

        If UpdateExtVars = True Then
            If NewRlZCap = True Then
                EVFileSuffix = "Updated"
            Else
                EVFileSuffix = ""
            End If
        End If

        'read in the elasticies
        Call ReadData("RailZone", "Elasticity", RlZoneEl, g_modelRunYear)

        'read in the strategy
        If TripRates = "SubStrategy" Then
            RlzTripRates = stratarray(1, 93)
        End If

    End Sub

    Sub LoadRlZInput()

        'Get ZoneID for the pop and gva functions
        'read the input data for the inputarray which is from the database
        If g_modelRunYear = g_initialYear Then
            'if it is initial year, read from the initial input
            RlZID(InputCount, 0) = InputArray(InputCount, 4)
            FareE(InputCount, 0) = InputArray(InputCount, 6)
            RlZTripsS(InputCount, 0) = InputArray(InputCount, 7)
            RlZPop(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, RlZID(InputCount, 0), "Zone", "'rail'")
            RlZGva(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, RlZID(InputCount, 0), "Zone", "'rail'")
            RlZCost(InputCount, 0) = InputArray(InputCount, 8)
            RlZStat(InputCount, 0) = InputArray(InputCount, 9)
            RlZCarFuel(InputCount, 0) = InputArray(InputCount, 10)
            RlZGJT(InputCount, 0) = InputArray(InputCount, 11)
        Else
            'if not year 1, read from the Input file
            RlZID(InputCount, 0) = InputArray(InputCount, 3)
            RlZPop(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, RlZID(InputCount, 0), "Zone", "'rail'")
            RlZGva(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, RlZID(InputCount, 0), "Zone", "'rail'")
            RlZCost(InputCount, 0) = RlZPreExtVar(InputCount, 6)
            RlZStat(InputCount, 0) = RlZPreExtVar(InputCount, 7)
            RlZCarFuel(InputCount, 0) = RlZPreExtVar(InputCount, 8)
            RlZGJT(InputCount, 0) = RlZPreExtVar(InputCount, 10)
            RlZTripsS(InputCount, 0) = InputArray(InputCount, 4)
            FareE(InputCount, 0) = InputArray(InputCount, 5)

        End If

    End Sub


    Sub RailZoneTrips()
        Dim PopRat, GVARat, FarRat, TrpRat, CFuelRat, GJTRat As Double
        Dim FarEl, GJTEl As Double
        Dim NewStatCount As Integer

        'Select the appropriate fare elasticity based on the FareE value
        Select Case FareE(InputCount, 0)
            Case 1
                FarEl = RlZoneEl(1, 3)
                GJTEl = RlZoneEl(1, 8)
            Case 2
                FarEl = RlZoneEl(1, 4)
                GJTEl = RlZoneEl(1, 9)
            Case 3
                FarEl = RlZoneEl(1, 5)
                GJTEl = RlZoneEl(1, 10)
            Case 4
                FarEl = RlZoneEl(1, 6)
                GJTEl = RlZoneEl(1, 11)
            Case Else
                '****otherwise stop the model and write an error to the log file
        End Select

        'Calculate the values of the various input ratios
        'now includes option to use variable elasticities
        If VariableEl = True Then
            OldX = RlZTripsS(InputCount, 0)
            'pop ratio
            OldY = RlZPop(InputCount, 0)
            If TripRates = "SubStrategy" Then
                NewY = RlZExtVar(InputCount, 4) * RlzTripRates
            Else
                NewY = RlZExtVar(InputCount, 4)
            End If
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(1, 1)
                Call VarElCalc()
                PopRat = VarRat
            Else
                If TripRates = "SubStrategy" Then
                    PopRat = ((RlZExtVar(InputCount, 4) * RlzTripRates) / RlZPop(InputCount, 0)) ^ RlZoneEl(1, 1)
                Else
                    PopRat = (RlZExtVar(InputCount, 4) / RlZPop(InputCount, 0)) ^ RlZoneEl(1, 1)
                End If

            End If
            'gva ratio
            OldY = RlZGva(InputCount, 0)
            NewY = RlZExtVar(InputCount, 5)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(1, 2)
                Call VarElCalc()
                GVARat = VarRat
            Else
                GVARat = (RlZExtVar(InputCount, 5) / RlZGva(InputCount, 0)) ^ RlZoneEl(1, 2)
            End If
            'fare ratio
            OldY = RlZCost(InputCount, 0)
            NewY = RlZExtVar(InputCount, 6)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = FarEl
                Call VarElCalc()
                FarRat = VarRat
            Else
                FarRat = (RlZExtVar(InputCount, 6) / RlZCost(InputCount, 0)) ^ FarEl
            End If
            'car fuel ratio
            OldY = RlZCarFuel(InputCount, 0)
            NewY = RlZExtVar(InputCount, 8)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = RlZoneEl(1, 7)
                Call VarElCalc()
                CFuelRat = VarRat
            Else
                CFuelRat = (RlZExtVar(InputCount, 8) / RlZCarFuel(InputCount, 0)) ^ RlZoneEl(1, 7)
            End If
            'GJT ratio
            OldY = RlZGJT(InputCount, 0)
            NewY = RlZExtVar(InputCount, 10)
            If Math.Abs((NewY / OldY) - 1) > ElCritValue Then
                OldEl = GJTEl
                Call VarElCalc()
                GJTRat = VarRat
            Else
                GJTRat = (RlZExtVar(InputCount, 10) / RlZGJT(InputCount, 0)) ^ GJTEl
            End If
        Else
            If TripRates = "SubStrategy" Then
                PopRat = ((RlZExtVar(InputCount, 4) * RlzTripRates) / RlZPop(InputCount, 0)) ^ RlZoneEl(1, 1)
            Else
                PopRat = (RlZExtVar(InputCount, 4) / RlZPop(InputCount, 0)) ^ RlZoneEl(1, 1)
            End If
            GVARat = (RlZExtVar(InputCount, 5) / RlZGva(InputCount, 0)) ^ RlZoneEl(1, 2)
            FarRat = (RlZExtVar(InputCount, 6) / RlZCost(InputCount, 0)) ^ FarEl
            CFuelRat = (RlZExtVar(InputCount, 8) / RlZCarFuel(InputCount, 0)) ^ RlZoneEl(1, 7)
            GJTRat = (RlZExtVar(InputCount, 9) / RlZGJT(InputCount, 0)) ^ GJTEl
        End If

        'Combine these ratios to get the trip ratio
        TrpRat = PopRat * GVARat * FarRat * CFuelRat * GJTRat

        'Multiply the trip ratio by the previous year's trips per station to get the new trips per station figure
        NewTripsS = RlZTripsS(InputCount, 0) * TrpRat

        'check if new stations have been added
        If RlZExtVar(InputCount, 7) > RlZStat(InputCount, 0) Then
            'if so then calculate how many
            NewStatCount = RlZExtVar(InputCount, 7) - RlZStat(InputCount, 0)
            'calculate the total number of trips in this situation
            NewTripTotal = (NewTripsS * RlZStat(InputCount, 0)) + (RlZExtVar(InputCount, 10) * NewStatCount)
            'recalculate the number of trips per station
            NewTripsS = NewTripTotal / (RlZStat(InputCount, 0) + NewStatCount)
        Else
            'otherwise just multiply new trips per station by number of stations to get total trips
            NewTripTotal = NewTripsS * RlZExtVar(InputCount, 7)
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
        OutputArray(InputCount, 0) = g_modelRunID
        OutputArray(InputCount, 1) = g_modelRunYear
        OutputArray(InputCount, 2) = RlZID(InputCount, 0)
        OutputArray(InputCount, 3) = NewTripsS
        OutputArray(InputCount, 4) = RlZExtVar(InputCount, 7)
        OutputArray(InputCount, 5) = NewTripTotal

        'update the variables
        RlZTripsS(InputCount, 0) = NewTripsS


        'write to the temp file
        TempArray(InputCount, 0) = g_modelRunID
        TempArray(InputCount, 1) = g_modelRunYear
        TempArray(InputCount, 2) = RlZID(InputCount, 0)
        TempArray(InputCount, 3) = RlZTripsS(InputCount, 0)
        TempArray(InputCount, 4) = FareE(InputCount, 0)

    End Sub



End Module
