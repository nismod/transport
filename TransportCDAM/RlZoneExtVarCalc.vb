Module RlZoneExtVarCalc
    '1.2 this version allows capacity changes to be included
    '1.2 it now allows external variable growth factors to be taken from an input file and to vary over time
    '1.3 this version allows input from the database
    'also includes fuel efficiency changes
    'now also includes electrification schemes
    '1.4 fuel efficiency calculation corrected
    'v1.6 now calculate by annual timesteps
    'correct the error that the original code doesnot read the electrificationdata correctly
    'note this module needs to run RlLinkExtVarCalc first
    'now all file related functions are using databaseinterface
    '1.9 now the module can run with database connection and read/write from/to database
    Dim InputRow As String
    Dim OutputRow As String
    Dim PopGrowth As Double
    Dim GVAGrowth As Double
    Dim CostGrowth As Double
    Dim FuelGrowth As Double
    Dim GJTGrowth As Double
    Dim ElPGrowth As Double
    Dim InputCount As Integer
    Dim CapID As Long
    Dim CapYear As Integer
    Dim InputData() As String
    Dim StationChange As Integer
    Dim TripChange As Double
    Dim ErrorString As String
    Dim stratstring As String
    Dim FuelEff(1), CO2Vol(1), CO2Price(1), GJTProp(1) As Double
    Dim ElectricZone, ElectricYear, ElectricStations As Long
    Dim FuelEffOld(2) As Double
    Dim enestring As String
    Dim Elect As Boolean
    Dim CapArray(,) As String
    Dim CapNum As Integer
    Dim elearray(,) As String
    Dim EleNum As Integer
    Dim ScalingData(,) As String
    Dim RlZ_InArray(,) As String
    Dim RlZEVRlZ_InArray(,) As String
    Dim RlZ_OutArray(145, 15) As String
    Dim yearIs2010 As Boolean = False




    Public Sub RlZoneEVMain()

        'for year 2010
        If g_modelRunYear = 2010 Then
            'create data for year 2010
            g_modelRunYear += 1
            'Call Year2010()
            yearIs2010 = True
            'Exit Sub
        End If

        'read all related files
        Call GetRlZEVFiles()

        'check if there is any value assigned to RlZEVSource - if not then set to constant as default
        If RlZEVSource = "" Then
            RlZEVSource = "Constant"
        End If

        'if we are using a single scaling factor then set scaling factors - as a default they are just set to be constant over time
        If RlZPopSource = "Constant" Then
            PopGrowth = 1.005
        End If
        If RlZEcoSource = "Constant" Then
            GVAGrowth = 1.01
        End If
        If RlZEneSource = "Constant" Then
            CostGrowth = 1.01
            FuelGrowth = 1.01
        End If
        If RlZEVSource = "Constant" Then
            GJTGrowth = 1.0
            'note that proportion of electric trains is scaled using an additive factor rather than a multiplicative one
            ElPGrowth = 0.01
        End If


        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        If NewRlZCap = True Then
            Call GetCapData()
        End If


        'initiallize read elelct file
        Elect = True

        'read first line from electrification file
        Call ElectricRead()

        'loop through rows in input data file calculating the external variable files, until there are no rows left
        Call CalcRlZExtVars()

        If yearIs2010 = True Then g_modelRunYear -= 1

    End Sub

    Sub GetRlZEVFiles()


        'read initial input of year 2010
        Call ReadData("RailZone", "Input", RlZ_InArray, 2011)

        If g_modelRunYear <> g_initialYear Then
            'read previous year's data
            Call ReadData("RailZone", "ExtVar", RlZEVRlZ_InArray, g_modelRunYear)
        End If

        'if capacity is changing then get capacity change file
        If NewRlZCap = True Then
            'read capchange info
            Call ReadData("RailZone", "CapChange", CapArray, g_modelRunYear)
            CapNum = 1
        End If

        'read ele scheme info
        Call ReadData("RailZone", "ElSchemes", elearray, g_modelRunYear)
        EleNum = 1

        'read file according to the setting
        If RlZOthSource = "File" Then
            Call ReadData("RailZone", "EVScale", ScalingData, g_modelRunYear)
        End If

        'v1.4 set fuel efficiency old to 1
        If g_modelRunYear = g_initialYear Then
            FuelEffOld(0) = 1
            FuelEffOld(1) = 1
        Else
            FuelEffOld(0) = stratarrayOLD(1, 68)
            FuelEffOld(1) = stratarrayOLD(1, 69)
        End If
        'v1.4 fuel efficiency change calculation corrected
        'read line from strategy array
        FuelEff(0) = stratarray(1, 68) / FuelEffOld(0)
        FuelEff(1) = stratarray(1, 69) / FuelEffOld(1)
        CO2Vol(0) = stratarray(1, 76)
        CO2Vol(1) = stratarray(1, 75)
        CO2Price(0) = stratarray(1, 72)
        CO2Price(1) = stratarray(1, 73)
        'also now get GJT growth value
        GJTProp(1) = stratarray(1, 84)
        'v1.4 update FuelEffOld values
        FuelEffOld(0) = stratarray(1, 68)
        FuelEffOld(1) = stratarray(1, 69)

    End Sub

    Sub CalcRlZExtVars()
        Dim ZoneID(238, 0) As Integer
        Dim PopOld(238, 0) As Double
        Dim GVAOld(238, 0) As Double
        Dim CostOld(238, 0) As Double
        Dim StationsOld(238, 0) As Double
        Dim FuelOld(238, 0) As Double
        Dim GJTOld(238, 0) As Double
        Dim ElPOld(238, 0) As Double
        Dim PopNew As Double
        Dim GVANew As Double
        Dim CostNew As Double
        Dim FuelNew As Double
        Dim StationsNew(238, 0) As Double
        Dim NewTrips As Double
        Dim GJTNew As Double
        Dim ElPNew As Double
        Dim DieselOld(238, 0), DieselNew, ElectricOld(238, 0), ElectricNew As Double
        Dim DMaintOld(238, 0), EMaintOld(238, 0) As Double
        Dim Country(238, 0) As String
        Dim y As Integer

        Dim diecarch, elecarch As Double
        Dim ElStat(238, 0) As Long
        Dim InputCount As Long


        'initialize values
        If RlZOthSource = "File" Then 'TODO - what is here?
        End If


        InputCount = 1


        Do Until InputCount > 144

            'read initial year input
            If g_modelRunYear = g_initialYear Then

                ZoneID(InputCount, 0) = RlZ_InArray(InputCount, 1)
                'read pop and gva by using database function
                PopOld(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, ZoneID(InputCount, 0), "Zone", "'rail'")
                GVAOld(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, ZoneID(InputCount, 0), "Zone", "'rail'")
                CostOld(InputCount, 0) = RlZ_InArray(InputCount, 4)
                StationsOld(InputCount, 0) = RlZ_InArray(InputCount, 5)
                FuelOld(InputCount, 0) = RlZ_InArray(InputCount, 6)
                GJTOld(InputCount, 0) = RlZ_InArray(InputCount, 7)
                Country(InputCount, 0) = RlZ_InArray(InputCount, 8)
                ElPOld(InputCount, 0) = RlZ_InArray(InputCount, 9)
                ElStat(InputCount, 0) = RlZ_InArray(InputCount, 10)
                NewTrips = 0

                'need to set StationsNew to equal StationsOld to start with, as it gets reset every year but doesn't change every year
                StationsNew(InputCount, 0) = StationsOld(InputCount, 0)

                'if using scaling factors then need to set a base value for the diesel fuel cost for this zone
                'can assume that 8.77% of total costs (which in all cases are set to 1) are made up of fuel, and that electric costs 55.3% of diesel price
                '0.0877 = (ElP * DieselPrice * 0.553) + (DP * DieselPrice)
                '0.0877 = DieselPrice((ElP * 0.553) + DP)
                'DieselPrice = 0.0877/(0.553ElP + DP)
                If RlZEneSource = "File" Then
                    DieselOld(InputCount, 0) = 0.0877 / ((0.553 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                    ElectricOld(InputCount, 0) = 0.553 * DieselOld(InputCount, 0)
                    'also need to set a base value for the maintenance and lease costs for this zone
                    'can assume that 26.62% of total costs (which in all cases are set to 1) are made up of maintenance and leasing, and that electric trains cost 75.8% of diesel trains
                    '0.2662 = (ElP * DMaint * 0.758) + (DP * DMaint)
                    '0.2662 = DMaint((ElP * 0.758) + DP)
                    'DMaint = 0.2662/(0.758ElP + DP)
                    DMaintOld(InputCount, 0) = 0.2662 / ((0.758 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                    EMaintOld(InputCount, 0) = 0.758 * DMaintOld(InputCount, 0)
                ElseIf RlZEneSource = "Database" Then
                    InDieselOld(InputCount, 0) = InDieselOldAll
                    InElectricOld(InputCount, 0) = InElectricOldAll
                    DieselOld(InputCount, 0) = 29.204
                    ElectricOld(InputCount, 0) = 16.156
                    DMaintOld(InputCount, 0) = 37.282
                    EMaintOld(InputCount, 0) = 24.855
                End If

            Else

                'update input parameters
                ZoneID(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 2)
                PopOld(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, ZoneID(InputCount, 0), "Zone", "'rail'")
                GVAOld(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, ZoneID(InputCount, 0), "Zone", "'rail'")
                CostOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 6)
                FuelOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 8)
                StationsOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 7)
                If RlZOthSource = "File" Then
                    GJTOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 10)
                End If
                Country(InputCount, 0) = RlZ_InArray(InputCount, 8)

                ElPOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 11)
                DieselOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 12)
                ElectricOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 13)
                If RlZEneSource = "Database" Then
                    InDieselOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 14)
                    InElectricOld(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 15)
                End If
                ElStat(InputCount, 0) = RlZEVRlZ_InArray(InputCount, 16)

                NewTrips = 0

                'also need to set a base value for the maintenance and lease costs for this zone
                If RlZEneSource = "File" Then
                    'can assume that 26.62% of total costs (which in all cases are set to 1) are made up of maintenance and leasing, and that electric trains cost 75.8% of diesel trains
                    '0.2662 = (ElP * DMaint * 0.758) + (DP * DMaint)
                    '0.2662 = DMaint((ElP * 0.758) + DP)
                    'DMaint = 0.2662/(0.758ElP + DP)
                    DMaintOld(InputCount, 0) = 0.2662 / ((0.758 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                    EMaintOld(InputCount, 0) = 0.758 * DMaintOld(InputCount, 0)
                ElseIf RlZEneSource = "Database" Then
                    DMaintOld(InputCount, 0) = 37.282
                    EMaintOld(InputCount, 0) = 24.855
                End If


            End If

            'if using scaling factors then read in the scaling factors for this year
            If RlZOthSource = "File" Then
                'need to leave cost growth factor until we know the new proportion of electric/diesel trains
                GJTGrowth = 1 + ScalingData(1, 8)
                ElPGrowth = ScalingData(1, 9)
            Else
Elect:          'altered to allow reading in electrification input file***
                If ZoneID(InputCount, 0) = ElectricZone Then
                    If g_modelRunYear = ElectricYear Then
                        ElPGrowth = 0.9 * (1 - ElPOld(InputCount, 0)) * (ElectricStations / (StationsOld(InputCount, 0) - ElStat(InputCount, 0)))
                        ElStat(InputCount, 0) += ElectricStations
                        Call ElectricRead()
                        If Elect = False Then
                            GoTo NextYear
                        End If
                    Else
                        ElPGrowth = 0
                    End If
                Else
                    ElPGrowth = 0
                End If

                'if there are multiply electricfication occur for the same zone and same year, go back to the lines before the do additional calculation
                'otherwise move forward
                If ZoneID(InputCount, 0) = ElectricZone Then
                    If g_modelRunYear = ElectricYear Then
                        GoTo Elect
                    Else
                        GoTo NextYear
                    End If
                End If
NextYear:
            End If

            If RlZPopSource = "Constant" Then
                PopNew = PopOld(InputCount, 0) * PopGrowth
            End If
            If RlZPopSource = "File" Then
                Select Case Country(InputCount, 0)
                    Case "E"
                        PopGrowth = 1 + ScalingData(1, 2)
                    Case "S"
                        PopGrowth = 1 + ScalingData(1, 3)
                    Case "W"
                        PopGrowth = 1 + ScalingData(1, 4)
                End Select
                PopNew = PopOld(InputCount, 0) * PopGrowth
            End If

            If RlZPopSource = "Database" Then
                'if year is after 2093 then no population forecasts are available so assume population remains constant
                'now modified as population data available up to 2100 - so should never need 'else'
                'v1.9 now read pop by using database function
                If g_modelRunYear < 91 Then
                    PopNew = get_population_data_by_zoneID(g_modelRunYear, ZoneID(InputCount, 0), "Zone", "'rail'")
                Else
                    PopNew = PopOld(InputCount, 0)
                End If
            End If
            If RlZEcoSource = "Constant" Then
                GVANew = GVAOld(InputCount, 0) * GVAGrowth
            ElseIf RlZEcoSource = "File" Then
                GVAGrowth = 1 + ScalingData(1, 5)
                GVANew = GVAOld(InputCount, 0) * GVAGrowth
            ElseIf RlZEcoSource = "Database" Then
                'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                'now modified as gva data available up to 2100 - so should never need 'else'
                'v1.9 now read gva by using database function
                If g_modelRunYear < 91 Then
                    GVANew = get_gva_data_by_zoneID(g_modelRunYear, ZoneID(InputCount, 0), "Zone", "'rail'")
                Else
                    GVANew = GVAOld(InputCount, 0)
                End If
            End If

            ElPNew = ElPOld(InputCount, 0) + ElPGrowth
            'constrain proportion of electric trains to 1
            If ElPNew > 1 Then
                ElPNew = 1
            End If

            'once we know new proportion of electric and diesel trains can calculate cost growth factor
            If RlZEneSource = "File" Then
                'fuel forms 8.77% of costs, and in base year electric costs are set as being 0.553 times diesel costs - base prices set above
                'scale both base prices
                DieselNew = DieselOld(InputCount, 0) * (1 + ScalingData(1, 6)) * FuelEff(1)
                ElectricNew = ElectricOld(InputCount, 0) * (1 + ScalingData(1, 7)) * FuelEff(0)
                ''maintenance and leasing forms 26.62% of total costs, and in base year electric costs are set as being 0.758 times diesel costs - base prices set above
                ''don't need to scale as assuming these costs remain constant per train over time
                '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                FuelGrowth = 1 + ScalingData(1, 6)
            ElseIf RlZEneSource = "Database" Then
                DieselNew = DieselOld(InputCount, 0) * (InDieselNew / InDieselOld(InputCount, 0)) * FuelEff(1)
                ElectricNew = ElectricOld(InputCount, 0) * (InElectricNew / InElectricOld(InputCount, 0)) * FuelEff(0)
                '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                FuelGrowth = InDieselNew / InDieselOld(InputCount, 0)
            ElseIf RlZEneSource = "Constant" Then
                DieselNew = DieselOld(InputCount, 0) * CostGrowth * FuelEff(1)
                ElectricNew = ElectricOld(InputCount, 0) * CostGrowth * FuelEff(0)
            End If

            'v1.4 if carbon charge is applied then calculate it
            If RlCaCharge = True Then
                'check if it is a relevant year
                If g_modelRunYear >= CarbChargeYear Then
                    'calculation is: (base fuel units per km * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
                    'as a base assuming that diesel trains use 1.873 litres/train km and electric trains use 12.611 kWh/train km
                    diecarch = 1.873 * FuelEff(1) * CO2Vol(1) * (CO2Price(1) / 10)
                    elecarch = 12.611 * FuelEff(0) * CO2Vol(0) * (CO2Price(0) / 10)
                Else
                    diecarch = 0
                    elecarch = 0
                End If
            Else
                diecarch = 0
                elecarch = 0
            End If


            'multiply new prices by new proportions and add to fixed costs
            CostNew = 121.381 + ((DieselNew + diecarch) * (1 - ElPNew)) + ((ElectricNew + elecarch) * ElPNew) + (EMaintOld(InputCount, 0) * ElPNew) + (DMaintOld(InputCount, 0) * (1 - ElPNew))

            FuelNew = FuelOld(InputCount, 0) * FuelGrowth
            If RlZOthSource = "File" Then
                GJTNew = GJTOld(InputCount, 0) * GJTGrowth
            Else
                GJTNew = GJTOld(InputCount, 0) * GJTProp(1)
            End If

            'if including capacity changes then check if there are any capacity changes on this flow
            If NewRlZCap = True Then
                'if there are any capacity changes on this flow, check if there are any capacity changes in this year
                If g_modelRunYear = CapYear Then
                    If ZoneID(InputCount, 0) = CapID Then
                        'if there are, then update the capacity variables, and read in the next row from the capacity file
                        StationsNew(InputCount, 0) = StationsOld(InputCount, 0) + StationChange
                        NewTrips = TripChange / StationChange
                        Call GetCapData()
                    End If
                End If
            End If

            If yearIs2010 = True Then g_modelRunYear -= 1

            'write to output file
            RlZ_OutArray(InputCount, 0) = g_modelRunID
            RlZ_OutArray(InputCount, 1) = ZoneID(InputCount, 0)
            RlZ_OutArray(InputCount, 2) = g_modelRunYear
            RlZ_OutArray(InputCount, 3) = PopNew
            RlZ_OutArray(InputCount, 4) = GVANew
            RlZ_OutArray(InputCount, 5) = CostNew
            RlZ_OutArray(InputCount, 6) = StationsNew(InputCount, 0)
            RlZ_OutArray(InputCount, 7) = FuelNew
            RlZ_OutArray(InputCount, 8) = NewTrips
            RlZ_OutArray(InputCount, 9) = GJTNew
            RlZ_OutArray(InputCount, 10) = ElPNew
            RlZ_OutArray(InputCount, 11) = DieselNew
            RlZ_OutArray(InputCount, 12) = ElectricNew
            RlZ_OutArray(InputCount, 13) = InDieselNew
            RlZ_OutArray(InputCount, 14) = InElectricNew
            RlZ_OutArray(InputCount, 15) = ElStat(InputCount, 0)

            If yearIs2010 = True Then g_modelRunYear += 1

            InputCount += 1
        Loop


        'create file if year 1, otherwise update
        'it is now writting to database, therefore no difference if it is year 1 or not
        If g_modelRunYear = g_initialYear Then
            Call WriteData("RailZone", "ExtVar", RlZ_OutArray, , True)
        Else
            Call WriteData("RailZone", "ExtVar", RlZ_OutArray, , False)
        End If



    End Sub

    Sub GetCapData()
        'read capacity data here
        If CapArray Is Nothing Then Exit Sub

        If CapArray(CapNum, 1) = "" Then
            'do nothing if reach the end
        Else
            CapID = CapArray(CapNum, 1)
            CapYear = CapArray(CapNum, 2)
            StationChange = CapArray(CapNum, 3)
            TripChange = CapArray(CapNum, 4)
            CapNum += 1
        End If
    End Sub

    Sub ElectricRead()
        'read electrification array here
        If elearray Is Nothing Then 'TODO - had to add this as elearray IS NOTHING - is this wrong? 'A: it is correct
            Elect = False
        ElseIf elearray(EleNum, 1) = "" Then
            ElectricZone = 0
            Elect = False
        Else
            ElectricZone = elearray(EleNum, 1)
            ElectricYear = elearray(EleNum, 2)
            ElectricStations = elearray(EleNum, 3)
            EleNum += 1
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
