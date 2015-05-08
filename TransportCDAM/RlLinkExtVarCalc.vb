Module RlLinkExtVarCalc
    'creates an external variables file for the rail model, based on a single year's input data and growth factors for the external variables
    '1.2 this version allows capacity changes to be specified
    '1.2 it now allows external variable growth factors to be taken from an input file and to vary over time
    '1.3 this version allows input from the database
    '1.4 now makes use of a list of electrification schemes
    'and also takes account of changing fuel efficiencies
    'and also gives the option of a carbon-based charge
    'now also includes rail zone electrification
    'v1.6 now calculate by annual timesteps
    'and InDieselOldAll, InElectricOldAll, InDieselNewAll, InElectricNewAll are created to replace the old parameters
    'and old InDieselOld/New and InElectricOld/New now increase dimension to seperate the values for each link
    'and each time the calculation start, the values in new parameters (InDieselOldAll etc) will be read into the original parameters
    'now all file related functions are using databaseinterface
    '1.9 now the module can run with database connection and read/write from/to database

    Dim InputCount As Integer
    Dim InputRow As String
    Dim InputData() As String
    Dim OutputRow As String
    Dim OPopGrowth, DPopGrowth As Double
    Dim OGVAGrowth, DGVAGrowth As Double
    Dim CostGrowth As Double
    Dim FuelGrowth As Double
    Dim MaxTDGrowth As Double
    Dim ElPGrowth As Double
    Dim CapID As Long
    Dim CapYear, CapNewYear As Integer
    Dim TrackChange As Integer
    Dim MaxTDChange As Double
    Dim ErrorString As String
    Dim ElectFlow, ElectYear, ElectTracks, ElectKm As Long
    Dim stratstring As String
    Dim FuelEff(1), CO2Vol(1), CO2Price(1), MaxTD As Double
    Dim OutString As String
    Dim CapCount As Double
    Dim AddingCap As Boolean
    Dim TracksToBuild, CapLanes As Double
    Dim CapType, CapRow As String
    Dim TrainChange As Double
    Dim NewCapDetails(455, 4) As Double
    Dim Breakout As Boolean
    Dim sortarray(11) As String
    Dim sortedline As String
    Dim splitline() As String
    Dim arraynum As Long
    Dim padflow, padyear As String
    Dim NewTrains As Double
    Dim FuelEffOld(2) As Double
    Dim Elect As Boolean
    Dim CapArray(455, 6) As String
    Dim NewCapArray(455, 6) As String
    Dim CapNum As Integer
    Dim RlL_InArray(,) As String
    Dim RlLEV_InArray(,) As String
    Dim RlL_OutArray(239, 17) As String
    Dim elearray(,) As String
    Dim EleNum As Integer
    Dim RlElNum As Integer
    Dim RzElNum As Integer
    Dim yearIs2010 As Boolean = False
    Dim RlL_TrackLength(,) As String





    Public Sub RailLinkEVMain()

        'for year 2010, skip calculation
        If g_modelRunYear = 2010 Then
            'create data for year 2010
            'g_modelRunYear += 1
            'Call Year2010()
            'yearIs2010 = True
            Exit Sub
        End If

        'check if there is any value assigned to RlLEVSource - if not then set to constant as default
        If RlLEVSource = "" Then
            RlLEVSource = "Constant"
        End If

        'if we are using a single scaling factor then set scaling factors - as a default they are just set to be constant over time
        If RlLPopSource = "Constant" Then
            OPopGrowth = 1.005
            DPopGrowth = 1.005
        End If
        If RlLEcoSource = "Constant" Then
            OGVAGrowth = 1.01
            DGVAGrowth = 1.01
        End If
        If RlLEneSource = "Constant" Then
            CostGrowth = 1.01
            FuelGrowth = 1.01
        End If
        If RlLEVSource = "Constant" Then
            MaxTDGrowth = 1
            'note that proportion of electric trains is scaled using an additive factor rather than a multiplicative one
            ElPGrowth = 0.025
        End If

        'read initial input data (year 2010)
        Call ReadData("RailLink", "Input", RlL_InArray, 2011)

        'read track length data in order to get the investment cost if there are tracks added in this year
        Call ReadData("RailLink", "TrackLength", RlL_TrackLength)

        If g_modelRunYear <> g_initialYear Then
            'read from previous year
            Call ReadData("RailLink", "ExtVar", RlLEV_InArray, (g_modelRunYear - 1))
        End If

        'only do the cap change calculation for the intermediate cap change file if it is year 1
        If yearIs2010 = False And g_modelRunYear = g_initialYear Then
            'read new capacity data
            Call ReadData("RailLink", "CapChange", CapArray, g_modelRunYear)

            'do capacity change requirement calculation
            Call CapChangeCalc()

            'write all lines from NewCapArray to intermediate capacity file
            If Not NewCapArray Is Nothing Then
                Call WriteData("RailLink", "NewCap", NewCapArray)
            End If
        End If

        'read new capacity info
        Call ReadData("RailLink", "NewCap", CapArray, g_modelRunYear)

        'reset NewCapArray row to the begining
        CapNum = 1
        CapID = 0
        'ReDim CapArray(238, 4)
        AddingCap = True
        Call GetCapData()

        'if including rail electrification then create the intermediate file sorted by flow then by date
        'mod - now do this anyway as some schemes are non-discretionary
        'create intermediate file listing timings of scheme implementations
        'create list only if it is year 1
        If yearIs2010 = False And g_modelRunYear = g_initialYear Then
            Call CreateElectrificationList()
        End If

        'initiallize read elelct file
        Elect = True
        'read the electrification list file as an input file
        'read ele scheme info
        Call ReadData("RailLink", "ElSchemes", elearray, g_modelRunYear)
        EleNum = 1
        Call GetElectData()

        'v1.4
        'get fuel efficiency and other values from the strategy file
        'v1.5 set fuel efficiency old to 1
        If g_modelRunYear = g_initialYear Then
            FuelEffOld(0) = 1
            FuelEffOld(1) = 1
        Else
            FuelEffOld(0) = stratarrayOLD(1, 68)
            FuelEffOld(1) = stratarrayOLD(1, 69)
        End If
        'v1.5 fuel efficiency change calculation corrected
        'read line from file
        FuelEff(0) = stratarray(1, 68) / FuelEffOld(0)
        FuelEff(1) = stratarray(1, 69) / FuelEffOld(1)
        CO2Vol(0) = stratarray(1, 76)
        CO2Vol(1) = stratarray(1, 75)
        CO2Price(0) = stratarray(1, 72)
        CO2Price(1) = stratarray(1, 73)
        MaxTD = stratarray(1, 80)


        'loop through rows in input data file calculating the external variable values
        Call CalcExtVars()

        'minus a year if it is year 2010, for the next module
        If yearIs2010 = True Then g_modelRunYear -= 1

    End Sub



    Sub CalcExtVars()

        Dim FlowID(238, 0) As Integer
        Dim Year As Integer
        Dim Tracks(238, 0) As Integer
        Dim Pop1Old(238, 0) As Double
        Dim Pop2Old(238, 0) As Double
        Dim GVA1Old(238, 0) As Double
        Dim GVA2Old(238, 0) As Double
        Dim CostOld(238, 0) As Double
        Dim FuelOld(238, 0) As Double
        Dim Pop1New As Double
        Dim Pop2New As Double
        Dim GVA1New As Double
        Dim GVA2New As Double
        Dim CostNew As Double
        Dim FuelNew As Double
        Dim MaxTDOld(238, 0), MaxTDNew As Double
        Dim DieselOld(238, 0), DieselNew, ElectricOld(238, 0), ElectricNew As Double
        Dim DMaintOld(238, 0), EMaintOld(238, 0) As Double
        Dim ElPOld(238, 0), ElPNew As Double
        Dim ScalingData(1, 9) As String
        Dim OCountry(238, 0), DCountry(238, 0) As String
        Dim OZone(238, 0), DZone(238, 0) As Long
        Dim keylookup As String
        Dim newval As Double
        Dim ElectTracksOld(238, 0), ElectTracksNew As Double
        Dim diecarch, elecarch As Double
        Dim Zone1ID As Integer, Zone2ID As Integer
        Dim y As Integer = 0

        'get scaling factor file if we are using one
        If RlLOthSource = True Then
            Call ReadData("RailLink", "EVScale", ScalingData, g_modelRunYear)
        End If


        InputCount = 1

        Do Until InputCount > 238

            'read from the initial data file if it is year 1 (calculation for year 2011, and initial data file is for 2010)
            If g_modelRunYear = g_initialYear Then
                FlowID(InputCount, 0) = RlL_InArray(InputCount, 1)
                OZone(InputCount, 0) = RlL_InArray(InputCount, 2)
                DZone(InputCount, 0) = RlL_InArray(InputCount, 3)
                Tracks(InputCount, 0) = RlL_InArray(InputCount, 4)
                Pop1Old(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, OZone(InputCount, 0), "OZ", "'rail'")
                Pop2Old(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, DZone(InputCount, 0), "DZ", "'rail'")
                GVA1Old(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, OZone(InputCount, 0), "OZ", "'rail'")
                GVA2Old(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, DZone(InputCount, 0), "DZ", "'rail'")
                CostOld(InputCount, 0) = RlL_InArray(InputCount, 7)
                FuelOld(InputCount, 0) = RlL_InArray(InputCount, 8)
                MaxTDOld(InputCount, 0) = RlL_InArray(InputCount, 9)
                ElPOld(InputCount, 0) = RlL_InArray(InputCount, 10)
                OCountry(InputCount, 0) = RlL_InArray(InputCount, 12)
                DCountry(InputCount, 0) = RlL_InArray(InputCount, 13)
                ElectTracksOld(InputCount, 0) = RlL_InArray(InputCount, 11)
                NewTrains = 0

                If RlLEneSource = "Database" Then
                    InDieselOld(InputCount, 0) = InDieselOldAll
                    InElectricOld(InputCount, 0) = InElectricOldAll
                    DieselOld(InputCount, 0) = 29.204
                    ElectricOld(InputCount, 0) = 16.156

                    DMaintOld(InputCount, 0) = 37.282
                    EMaintOld(InputCount, 0) = 24.855

                Else
                    'can assume that 8.77% of total costs (which in all cases are set to 1) are made up of fuel, and that electric costs 55.3% of diesel price
                    '0.0877 = (ElP * DieselPrice * 0.553) + (DP * DieselPrice)
                    '0.0877 = DieselPrice((ElP * 0.553) + DP)
                    'DieselPrice = 0.0877/(0.553ElP + DP)
                    DieselOld(InputCount, 0) = 0.0877 / ((0.553 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                    ElectricOld(InputCount, 0) = 0.553 * DieselOld(InputCount, 0)
                    'also need to set a base value for the maintenance and lease costs for this zone
                    'can assume that 26.62% of total costs (which in all cases are set to 1) are made up of maintenance and leasing, and that electric trains cost 75.8% of diesel trains
                    '0.2662 = (ElP * DMaint * 0.758) + (DP * DMaint)
                    '0.2662 = DMaint((ElP * 0.758) + DP)
                    'DMaint = 0.2662/(0.758ElP + DP)
                    DMaintOld(InputCount, 0) = 0.2662 / ((0.758 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                    EMaintOld(InputCount, 0) = 0.758 * DMaintOld(InputCount, 0)

                End If

            Else
                'read from previous year's data
                FlowID(InputCount, 0) = RlL_InArray(InputCount, 1)
                OZone(InputCount, 0) = RlL_InArray(InputCount, 2)
                DZone(InputCount, 0) = RlL_InArray(InputCount, 3)
                Tracks(InputCount, 0) = RlLEV_InArray(InputCount, 4)
                Pop1Old(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, OZone(InputCount, 0), "OZ", "'rail'")
                Pop2Old(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear - 1, DZone(InputCount, 0), "DZ", "'rail'")
                GVA1Old(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, OZone(InputCount, 0), "OZ", "'rail'")
                GVA2Old(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear - 1, DZone(InputCount, 0), "DZ", "'rail'")

                CostOld(InputCount, 0) = RlLEV_InArray(InputCount, 9)
                FuelOld(InputCount, 0) = RlLEV_InArray(InputCount, 10)
                MaxTDOld(InputCount, 0) = RlLEV_InArray(InputCount, 11)
                ElPOld(InputCount, 0) = RlLEV_InArray(InputCount, 12)
                ElectTracksOld(InputCount, 0) = RlLEV_InArray(InputCount, 13)
                OCountry(InputCount, 0) = RlL_InArray(InputCount, 13)
                DCountry(InputCount, 0) = RlL_InArray(InputCount, 14)

                DieselOld(InputCount, 0) = RlLEV_InArray(InputCount, 15)
                ElectricOld(InputCount, 0) = RlLEV_InArray(InputCount, 16)

                If RlLEneSource = "Database" Then
                    InDieselOld(InputCount, 0) = RlLEV_InArray(InputCount, 17)
                    InElectricOld(InputCount, 0) = RlLEV_InArray(InputCount, 18)

                    DMaintOld(InputCount, 0) = 37.282
                    EMaintOld(InputCount, 0) = 24.855

                Else
                    DMaintOld(InputCount, 0) = 0.2662 / ((0.758 * ElPOld(InputCount, 0)) + (1 - ElPOld(InputCount, 0)))
                    EMaintOld(InputCount, 0) = 0.758 * DMaintOld(InputCount, 0)

                End If

                NewTrains = 0

            End If

            If RlLPopSource = "Constant" Then
                Pop1New = Pop1Old(InputCount, 0) * OPopGrowth
                Pop2New = Pop2Old(InputCount, 0) * DPopGrowth
            ElseIf RlLPopSource = "File" Then
                Select Case OCountry(InputCount, 0)
                    'Case "E"
                    Case "1"
                        OPopGrowth = 1 + ScalingData(1, 2)
                        'Case "S"
                    Case "3"
                        OPopGrowth = 1 + ScalingData(1, 3)
                        'Case "w"
                    Case "2"
                        OPopGrowth = 1 + ScalingData(1, 4)
                End Select
                Select Case DCountry(InputCount, 0)
                    'Case "E"
                    Case "1"
                        DPopGrowth = 1 + ScalingData(1, 2)
                        'Case "S"
                    Case "3"
                        DPopGrowth = 1 + ScalingData(1, 3)
                        'Case "W"
                    Case "2"
                        DPopGrowth = 1 + ScalingData(1, 4)
                End Select
                Pop1New = Pop1Old(InputCount, 0) * OPopGrowth
                Pop2New = Pop2Old(InputCount, 0) * DPopGrowth
            ElseIf RlLPopSource = "Database" Then
                'if year is after 2093 then no population forecasts are available so assume population remains constant
                'now modified as population data available up to 2100 - so should never need 'else'
                'v1.9 now read by using database function
                If g_modelRunYear < 2101 Then
                    Pop1New = get_population_data_by_zoneID(g_modelRunYear, OZone(InputCount, 0), "OZ", "'rail'")
                    Pop2New = get_population_data_by_zoneID(g_modelRunYear, DZone(InputCount, 0), "DZ", "'rail'")
                Else
                    Pop1New = Pop1Old(InputCount, 0)
                    Pop2New = Pop2Old(InputCount, 0)
                End If
            End If
            If RlLEcoSource = "Constant" Then
                GVA1New = GVA1Old(InputCount, 0) * OGVAGrowth
                GVA2New = GVA2Old(InputCount, 0) * DGVAGrowth
            ElseIf RlLEcoSource = "File" Then
                OGVAGrowth = 1 + ScalingData(1, 5)
                DGVAGrowth = 1 + ScalingData(1, 5)
                GVA1New = GVA1Old(InputCount, 0) * OGVAGrowth
                GVA2New = GVA2Old(InputCount, 0) * DGVAGrowth
            ElseIf RlLEcoSource = "Database" Then
                'if year is after 2050 then no gva forecasts are available so assume gva remains constant
                'now modified as GVA data available up to 2100 - so should never need 'else'
                'v1.9 now read by using database function
                'database does not have gva forecasts after year 2050, and the calculation is only available before year 2050
                If g_modelRunYear < 2101 Then
                    GVA1New = get_gva_data_by_zoneID(g_modelRunYear, OZone(InputCount, 0), "OZ", "'rail'")
                    GVA2New = get_gva_data_by_zoneID(g_modelRunYear, DZone(InputCount, 0), "DZ", "'rail'")
                Else
                    GVA1New = GVA1Old(InputCount, 0)
                    GVA2New = GVA2Old(InputCount, 0)
                End If
            End If
            'need to leave cost growth factor until we know new proportion of electric/diesel trains
            If RlLOthSource = True Then
                'MaxTDGrowth = 1 + ScalingData(1,7)
                ElPGrowth = ScalingData(1, 9)
            End If

            'check if using list of electrification schemes
            ''mod - now do this anyway as some schemes are non-discretionary
            'If RlElect = True Then
            'check if in correct year for the current scheme
Elect:
            If g_modelRunYear = ElectYear Then
                'if so check if correct row for the current scheme
                If FlowID(InputCount, 0) = ElectFlow Then
                    'if so, then need to alter proportions of diesel and electric trains
                    ElPNew = ElPOld(InputCount, 0) + (0.9 * ((1 - ElPOld(InputCount, 0)) * (ElectTracks / (Tracks(InputCount, 0) - ElectTracksOld(InputCount, 0)))))
                    ElectTracksNew = ElectTracksOld(InputCount, 0) + ElectTracks

                    'write to CrossSector output for investment cost
                    'Rail electrification: £1.00 million per track km
                    crossSectorArray(1, 3) += 1 * ElectTracks * ElectKm


                    'read next scheme from list
                    Call GetElectData()

                    'if there is no data left in the Elect file then go to next calculation
                    If Elect = False Then
                        GoTo NextYear
                    End If

                Else
                    ElPNew = ElPOld(InputCount, 0)
                    ElectTracksNew = ElectTracksOld(InputCount, 0)
                End If
            Else
                ElPNew = ElPOld(InputCount, 0)
                ElectTracksNew = ElectTracksOld(InputCount, 0)
            End If

            If FlowID(InputCount, 0) = ElectFlow Then
                If g_modelRunYear = ElectYear Then
                    GoTo Elect
                Else
                    GoTo NextYear
                End If
            End If
NextYear:
            'Else
            '    '***1.4 commented out, as don't want any growth if not using list of schemes
            '    'ElPNew = ElPOld + ElPGrowth#
            '    ElPNew = ElPOld
            '    ElectTracksNew = ElectTracksOld
            'End If
            'constrain proportion of electric trains to 1
            If ElPNew > 1 Then
                ElPNew = 1
            End If
            'once we know new proportion of electric and diesel trains can calculate cost growth factor
            If RlLEneSource = "File" Then
                'fuel forms 8.77% of costs, and in base year electric costs are set as being 0.553 times diesel costs - base prices set above
                'scale both base prices
                DieselNew = DieselOld(InputCount, 0) * (1 + ScalingData(1, 6)) * FuelEff(1)
                ElectricNew = ElectricOld(InputCount, 0) * (1 + ScalingData(1, 7)) * FuelEff(0)
                '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                FuelGrowth = 1 + ScalingData(1, 6)
            ElseIf RlLEneSource = "Constant" Then
                DieselNew = DieselOld(InputCount, 0) * CostGrowth * FuelEff(1)
                ElectricNew = ElectricOld(InputCount, 0) * CostGrowth * FuelEff(0)
            ElseIf RlLEneSource = "Database" Then
                InDieselNew = InDieselNewAll
                InElectricNew = InElectricNewAll
                DieselNew = DieselOld(InputCount, 0) * (InDieselNew / InDieselOld(InputCount, 0)) * FuelEff(1)
                ElectricNew = ElectricOld(InputCount, 0) * (InElectricNew / InElectricOld(InputCount, 0)) * FuelEff(0)
                '*****this assumes car fuel costs are only based on oil prices - when really we need to integrate this with the road model to look at road fuel/split
                FuelGrowth = InDieselNew / InDieselOld(InputCount, 0)
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

            'maintenance and leasing forms 26.62% of total costs, and in base year electric costs are set as being 0.758 times diesel costs - base prices set above
            'don't need to scale as assuming these costs remain constant per train over time
            'multiply new prices by new proportions and add to fixed costs
            'v1.4 replaced old fixed costs of 0.6461 with fixed cost of 121.381p
            CostNew = 121.381 + ((DieselNew + diecarch) * (1 - ElPNew)) + ((ElectricNew + elecarch) * ElPNew) + (EMaintOld(InputCount, 0) * ElPNew) + (DMaintOld(InputCount, 0) * (1 - ElPNew))
            'estimate new fuel efficiency for road vehicles
            FuelNew = FuelOld(InputCount, 0) * FuelGrowth
            'if including capacity changes, then check if there are any capacity changes on this flow
            'v1.4 changed to include compulsory capacity changes where construction has already begun
            'all this involves is removing the if newrllcap = true clause, because this was already accounted for when generating the intermediate file, and adding a lineread above getcapdata because this sub was amended

            'TODO - how does this work with the years = 200+??? -DONE no data available to years = 200+
            'if there are any capacity changes on this flow, check if there are any capacity changes in this year
            If g_modelRunYear = CapYear Then
                If FlowID(InputCount, 0) = CapID Then
                    'if there are, then update the capacity variables, and read in the next row from the capacity file
                    Tracks(InputCount, 0) += TrackChange
                    'note that MaxTDChange now doesn't work - replaced by strategy common variables file
                    MaxTDOld(InputCount, 0) += MaxTDChange
                    NewTrains = TrainChange
                    'write to CrossSector output for investment cost
                    'Railways: £18.64 million per track km (not route km)
                    crossSectorArray(1, 3) += 18.64 * TrackChange * RlL_TrackLength(InputCount + 1, 4) 'the inputcount must be +1, as the first row is for id = -1 in the table

                    Call GetCapData()
                End If
            End If
            MaxTDNew = MaxTD

            If yearIs2010 = True Then g_modelRunYear -= 1

            'write to output file
            RlL_OutArray(InputCount, 0) = g_modelRunID
            RlL_OutArray(InputCount, 1) = FlowID(InputCount, 0)
            RlL_OutArray(InputCount, 2) = g_modelRunYear
            RlL_OutArray(InputCount, 3) = Tracks(InputCount, 0)
            RlL_OutArray(InputCount, 4) = Pop1New
            RlL_OutArray(InputCount, 5) = Pop2New
            RlL_OutArray(InputCount, 6) = GVA1New
            RlL_OutArray(InputCount, 7) = GVA2New
            RlL_OutArray(InputCount, 8) = CostNew
            RlL_OutArray(InputCount, 9) = FuelNew
            RlL_OutArray(InputCount, 10) = MaxTDNew
            RlL_OutArray(InputCount, 11) = ElPNew
            RlL_OutArray(InputCount, 12) = ElectTracksNew
            RlL_OutArray(InputCount, 13) = NewTrains
            RlL_OutArray(InputCount, 14) = DieselNew
            RlL_OutArray(InputCount, 15) = ElectricNew
            If RlLEneSource = "Database" Then
                RlL_OutArray(InputCount, 16) = InDieselNew
                RlL_OutArray(InputCount, 17) = InElectricNew
            End If

            'add back a year for next zone/link
            If yearIs2010 = True Then g_modelRunYear += 1

            'next link
            InputCount += 1
        Loop

        'create file if year 1, otherwise update
        'it is now writting to database, therefore no difference if it is year 1 or not
        If g_modelRunYear = g_initialYear Then
            Call WriteData("RailLink", "ExtVar", RlL_OutArray, , True)
        Else
            Call WriteData("RailLink", "ExtVar", RlL_OutArray, , False)
        End If




    End Sub

    Sub GetCapData()

        If CapArray Is Nothing Then Exit Sub

        If CapArray(CapNum, 1) Is Nothing Then
        Else
            CapID = CapArray(CapNum, 2)
            If CapArray(CapNum, 3) = "-1" Then
                CapYear = -1
            Else
                If AddingCap = False Then
                    CapYear = CapArray(CapNum, 3)
                Else
                    CapYear = CapArray(CapNum, 3)
                End If
            End If
            TrackChange = CapArray(CapNum, 4)
            MaxTDChange = CapArray(CapNum, 5)
            TrainChange = CapArray(CapNum, 6)
            If AddingCap = False Then
                CapType = CapArray(CapNum, 7)
            End If
            CapNum += 1
        End If

    End Sub


    Sub CreateElectrificationList()
        'now modified to include some schemes as standard
        'now modified to include zones as well as links

        Dim schemeoutputrow(244, 4) As String
        Dim elschemes(244, 3) As Double
        Dim schemearray(245, 6) As String
        Dim rownum As Integer
        Dim elyear As Long
        Dim eltrackkm As Double
        Dim kmtoelectrify As Double
        Dim sortarray(244) As String
        Dim sortedline As String
        Dim splitline() As String
        Dim arraynum, schemecount As Long
        Dim schemetype As String
        Dim zoneoutputrow(335, 3) As String
        Dim zonearray(336, 5) As String
        Dim schemecode As String
        Dim elzschemes(335, 4) As Long
        Dim znum As Integer
        Dim zonecheck As Boolean

        'read old link scheme file
        Call ReadData("RailLink", "OldRlEl", schemearray, g_modelRunYear)

        'read old zone scheme file
        Call ReadData("RailLink", "OldRzEl", zonearray, g_modelRunYear)

        kmtoelectrify = 0

        'initialize the Railink and Railzone electrification array
        RlElNum = 1
        RzElNum = 1

        rownum = 0
        schemecount = -1
        'read details of first zone scheme
        znum = 0
        zonecheck = True
        'loop through all rows in the initial file assigning a year if schemes don't yet have one and writing values to array

        If Not schemearray Is Nothing Then
            Do Until RlElNum > 244
                'check the scheme type
                schemetype = schemearray(RlElNum, 6)
                If schemetype = "C" Then
                    'if it is a compulsory scheme then load values into array
                    For v = 0 To 3
                        elschemes(rownum, v) = schemearray(RlElNum, v + 1)
                    Next
                    elyear = schemearray(RlElNum, 2)
                    schemecount += 1
                    'now check for relevant zone changes
                    schemecode = schemearray(RlElNum, 5)
                    Do While zonecheck = True
                        If schemecode = zonearray(RzElNum, 4) Then
                            elzschemes(znum, 0) = zonearray(RzElNum, 1)
                            elzschemes(znum, 1) = zonearray(RzElNum, 2)
                            elzschemes(znum, 2) = zonearray(RzElNum, 3)
                            RzElNum += 1
                            If zonearray(RzElNum, 1) Is Nothing Then
                                zonecheck = False
                            Else
                                znum += 1
                                zonecheck = True
                            End If
                        Else
                            zonecheck = False
                        End If
                    Loop
                Else
                    'check if we are using optional electrification schemes
                    If RlElect = True Then
                        'check if a year is already assigned
                        If schemearray(RlElNum, 2) = "" Then
                            'if it isn't then first get the length of track km for the scheme 
                            eltrackkm = schemearray(RlElNum, 3) * schemearray(RlElNum, 4)
                            'then check if there are any spare electrification km in the pot
                            If kmtoelectrify >= eltrackkm Then
                                'if there are enough, then assign this scheme to this year and load values into array
                                elschemes(rownum, 0) = schemearray(RlElNum, 1)
                                elschemes(rownum, 1) = elyear
                                elschemes(rownum, 2) = schemearray(RlElNum, 3)
                                elschemes(rownum, 3) = schemearray(RlElNum, 4)
                                'subtract the electrified km from the spare km
                                kmtoelectrify = kmtoelectrify - eltrackkm
                                schemecount += 1
                                schemecode = schemearray(RlElNum, 5)
                                Do While zonecheck = True
                                    If schemecode = zonearray(RzElNum, 4) Then
                                        elzschemes(znum, 0) = zonearray(RzElNum, 1)
                                        elzschemes(znum, 1) = elyear
                                        elzschemes(znum, 2) = zonearray(RzElNum, 3)
                                        RzElNum += 1
                                        If zonearray(RzElNum, 1) Is Nothing Then
                                            zonecheck = False
                                        Else
                                            znum += 1
                                            zonecheck = True
                                        End If
                                    Else
                                        zonecheck = False
                                    End If
                                Loop
                            Else
                                'if there aren't, then move on to next year and add in a further allocation of track km
                                'loop until there are enough km in the pot to electrify the scheme
                                Do Until kmtoelectrify >= eltrackkm
                                    elyear += 1
                                    If elyear > 2100 Then
                                        Exit Do
                                    End If
                                    kmtoelectrify += ElectKmPerYear
                                Loop
                                'check if enough track km - if there aren't then it means we have reached 2100 so exit do loop
                                If kmtoelectrify >= eltrackkm Then
                                    'if there are enough, then assign this scheme to this year and load values into array
                                    elschemes(rownum, 0) = schemearray(RlElNum, 1)
                                    elschemes(rownum, 1) = elyear
                                    elschemes(rownum, 2) = schemearray(RlElNum, 3)
                                    elschemes(rownum, 3) = schemearray(RlElNum, 4)
                                    'subtract the electrified km from the spare km
                                    kmtoelectrify = kmtoelectrify - eltrackkm
                                    schemecount += 1
                                    schemecode = schemearray(RlElNum, 5)
                                    Do While zonecheck = True
                                        If schemecode = zonearray(RzElNum, 4) Then
                                            elzschemes(znum, 0) = zonearray(RzElNum, 1)
                                            elzschemes(znum, 1) = elyear
                                            elzschemes(znum, 2) = zonearray(RzElNum, 3)
                                            RzElNum += 1
                                            If zonearray(RzElNum, 1) Is Nothing Then
                                                zonecheck = False
                                            Else
                                                znum += 1
                                                zonecheck = True
                                            End If
                                        Else
                                            zonecheck = False
                                        End If
                                    Loop
                                Else
                                    Exit Do
                                End If
                            End If
                        Else
                            'if it is then load values into array
                            For v = 0 To 3
                                elschemes(rownum, v) = schemearray(RlElNum, v + 1)
                            Next
                            elyear = schemearray(RlElNum, 2)
                            schemecount += 1
                            schemecode = schemearray(RlElNum, 5)
                            Do While zonecheck = True
                                If schemecode = zonearray(RzElNum, 4) Then
                                    elzschemes(znum, 0) = zonearray(RzElNum, 1)
                                    elzschemes(znum, 1) = zonearray(RzElNum, 2)
                                    elzschemes(znum, 2) = zonearray(RzElNum, 3)
                                    RzElNum += 1
                                    If zonearray(RzElNum, 1) Is Nothing Then
                                        zonecheck = False
                                    Else
                                        znum += 1
                                        zonecheck = True
                                    End If
                                Else
                                    zonecheck = False
                                End If
                            Loop
                        End If
                    End If
                End If
                'read next line from input file
                RlElNum += 1
                rownum += 1
                zonecheck = True
            Loop
        End If

        'now need to sort the array by flow id then by year
        ReDim sortarray(schemecount)
        For v = 0 To schemecount
            'concatenate the relevant values (first flow id, then year, then main array position) into a single dimension string array
            padflow = String.Format("{0:000}", elschemes(v, 0))
            padyear = String.Format("{0:00}", elschemes(v, 1))
            sortarray(v) = padyear & "&" & padflow & "&" & v
        Next
        'sort this array
        Array.Sort(sortarray)
        'then go through the sorted values getting the relevant information from the main array and writing to the output file
        RlElNum = 1
        For v = 0 To schemecount
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            'skip lines which don't correspond to a flow
            If elschemes(arraynum, 0) > 0 Then
                schemeoutputrow(RlElNum, 0) = g_modelRunID
                schemeoutputrow(RlElNum, 1) = elschemes(arraynum, 0)
                schemeoutputrow(RlElNum, 2) = elschemes(arraynum, 1)
                schemeoutputrow(RlElNum, 3) = elschemes(arraynum, 2)
                schemeoutputrow(RlElNum, 4) = elschemes(arraynum, 3)
                RlElNum += 1
            End If
        Next
        'now need to sort the zone array by zone id then by year
        ReDim sortarray(znum - 1)
        For v = 0 To (znum - 1)
            'concatenate the relevant values (first zone id, then year, then main array position) into a single dimension string array
            padflow = String.Format("{0:000}", elzschemes(v, 0))
            padyear = String.Format("{0:00}", elzschemes(v, 1))
            sortarray(v) = padyear & "&" & padflow & "&" & v
        Next
        'sort this array
        Array.Sort(sortarray)
        'then go through the sorted values getting the relevant information from the main array and writing to the output file
        RzElNum = 1
        For v = 0 To (znum - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            'skip lines which have a zero station count
            If elzschemes(arraynum, 2) > 0 Then
                zoneoutputrow(RzElNum, 0) = g_modelRunID
                zoneoutputrow(RzElNum, 1) = elzschemes(arraynum, 0)
                zoneoutputrow(RzElNum, 2) = elzschemes(arraynum, 1)
                zoneoutputrow(RzElNum, 3) = elzschemes(arraynum, 2)
                RzElNum += 1
            End If
        Next

        Call WriteData("RailLink", "ElSchemes", schemeoutputrow)
        Call WriteData("RailZone", "ElSchemes", zoneoutputrow)

    End Sub

    Sub GetElectData()
        'read electrification data here
        If elearray Is Nothing Then
            Elect = False ' Need to Log these errors
        ElseIf elearray(EleNum, 2) = "" Then
            Elect = False
        Else
            ElectFlow = elearray(EleNum, 2)
            ElectYear = elearray(EleNum, 3)
            ElectTracks = elearray(EleNum, 4)
            ElectKm = elearray(EleNum, 5)
            EleNum += 1
        End If
    End Sub

    Sub CapChangeCalc()

        'start from the first row of CapArray
        CapNum = 1

        If CapArray Is Nothing Then Exit Sub

        'need initial file to be sorted by scheme type then by change year then by order of priority
        'first read all compulsory enhancements to intermediate array
        CapCount = 0
        'addingcap is false when is reading from LU table
        AddingCap = False
        TracksToBuild = 0
        Do Until CapArray(CapNum, 1) Is Nothing

            If CapArray(CapNum, 1) Is Nothing Then
            Else
                CapID = CapArray(CapNum, 1)
                If CapArray(CapNum, 2) = "-1" Then
                    CapYear = -1
                Else
                    If AddingCap = False Then
                        CapYear = CapArray(CapNum, 2)
                    Else
                        CapYear = CapArray(CapNum, 2)
                    End If
                End If
                TrackChange = CapArray(CapNum, 3)
                MaxTDChange = CapArray(CapNum, 4)
                TrainChange = CapArray(CapNum, 5)
                If AddingCap = False Then
                    CapType = CapArray(CapNum, 6)
                End If
                CapNum += 1
            End If


            Select Case CapType
                Case "C"
                    NewCapDetails(CapCount, 0) = CapID
                    NewCapDetails(CapCount, 1) = CapYear
                    NewCapDetails(CapCount, 2) = TrackChange
                    NewCapDetails(CapCount, 3) = MaxTDChange
                    NewCapDetails(CapCount, 4) = TrainChange
                    CapNewYear = CapYear
                Case "O"
                    'then if adding optional capacity read all optional dated enhancements to intermediate array
                    If NewRlLCap = True Then
                        If CapYear >= 0 Then
                            NewCapDetails(CapCount, 0) = CapID
                            NewCapDetails(CapCount, 1) = CapYear
                            NewCapDetails(CapCount, 2) = TrackChange
                            NewCapDetails(CapCount, 3) = MaxTDChange
                            NewCapDetails(CapCount, 4) = TrainChange
                            CapNewYear = CapYear
                        Else
                            'finally add all other enhancements to intermediate array until we have run out of additional capacity
                            If TracksToBuild >= TrackChange Then
                                NewCapDetails(CapCount, 0) = CapID
                                NewCapDetails(CapCount, 1) = CapNewYear
                                NewCapDetails(CapCount, 2) = TrackChange
                                NewCapDetails(CapCount, 3) = MaxTDChange
                                NewCapDetails(CapCount, 4) = TrainChange
                                TracksToBuild = TracksToBuild - TrackChange
                            Else
                                Do Until TracksToBuild >= TrackChange
                                    CapNewYear += 1
                                    If CapNewYear > 2100 Then
                                        Breakout = True
                                        Exit Select
                                    End If
                                    TracksToBuild += NewRailTracks
                                Loop
                                NewCapDetails(CapCount, 0) = CapID
                                NewCapDetails(CapCount, 1) = CapNewYear
                                NewCapDetails(CapCount, 2) = TrackChange
                                NewCapDetails(CapCount, 3) = MaxTDChange
                                NewCapDetails(CapCount, 4) = TrainChange
                                TracksToBuild = TracksToBuild - TrackChange
                            End If
                        End If
                    Else
                        Exit Do
                    End If
            End Select
            'exit if year is greater than our range (90 years)
            If Breakout = True Then
                Exit Do
            End If
            CapCount += 1
        Loop


        'then sort the intermediate array by year, then by flow ID of implementation
        ReDim sortarray(CapCount - 1)
        For v = 0 To (CapCount - 1)
            padflow = String.Format("{0:000}", NewCapDetails(v, 0))
            padyear = String.Format("{0:00}", NewCapDetails(v, 1))
            sortarray(v) = padyear & "&" & padflow & "&" & v
        Next
        Array.Sort(sortarray)

        'write all lines to NewCapArray
        For v = 0 To (CapCount - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            NewCapArray(v + 1, 0) = g_modelRunID
            NewCapArray(v + 1, 1) = NewCapDetails(arraynum, 0)
            NewCapArray(v + 1, 2) = NewCapDetails(arraynum, 1)
            NewCapArray(v + 1, 3) = NewCapDetails(arraynum, 2)
            NewCapArray(v + 1, 4) = NewCapDetails(arraynum, 3)
            NewCapArray(v + 1, 5) = NewCapDetails(arraynum, 4)
        Next

    End Sub
End Module
