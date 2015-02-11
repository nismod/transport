﻿Module RZExtVarCalc
    'creates an external variables file for the intrazonal road model, based on a single year's input data and growth factors for the other variables
    '1.2 this version allows capacity changes to be specified
    '1.2 it also disaggregates lane km by road type and specifies vehicle fuel proportions
    '1.3 this version allows input from the database, and makes use of a general strategy file for fuel split values
    '1.3 also now breaks down and calculates the cost variable
    '1.3 now also includes option to impose a workplace parking levy
    '1.3 now includes capacity enhancements automatically
    '1.4 fuel efficiency and cost calculations corrected
    '1.6 Recode to calculate by annual timesteps, the dimension of the parameter array has been increased by one to store previous year's data
    'may need to check if the code works if RdZEneSource is not = "Database"
    'now all file related functions are using databaseinterface
    '1.9 now the module can run with database connection and read/write from/to database

    Dim InputRow As String
    Dim OutputRow As String
    Dim InputData() As String
    Dim PopGrowth As Double
    Dim GVAGrowth As Double
    Dim CostGrowth As Double
    Dim CapID As Long
    Dim CapYear As Integer
    Dim MwayKmChange As Double
    Dim RurADKmChange As Double
    Dim RurASKmChange As Double
    Dim RurMinKmChange As Double
    Dim UrbDKmChange As Double
    Dim UrbSKmChange As Double
    Dim ErrorString As String
    Dim FuelEffOld(144, 34), FuelEffNew(34), FuelEffChange(34) As Double
    Dim Year, WPPLStart As Long
    Dim enestring As String
    Dim InputCount As Long
    Dim capstring As String
    Dim caparray(13670, 9) As String
    Dim capnum, zonecapcount As Long
    Dim zonecapdetails(13670, 8) As Double
    Dim sortarray(13670) As String
    Dim padzone, padyear As String
    Dim sortedline As String
    Dim splitline() As String
    Dim arraynum As Long
    Dim stratstring As String
    Dim stratarray(90, 95) As String
    Dim ZoneID(144, 0) As String
    Dim PopOld(144, 0) As Double
    Dim GVAOld(144, 0) As Double
    Dim CostOld(144, 4) As Double
    Dim PopNew As Double
    Dim GVANew As Double
    Dim CostNew(4) As Double
    Dim LaneKm(144, 0) As Double
    Dim MLaneKm(144, 0) As Double
    Dim RurADLaneKm(144, 0) As Double
    Dim RurASLaneKm(144, 0) As Double
    Dim RurMinLaneKm(144, 0) As Double
    Dim UrbDLaneKm(144, 0) As Double
    Dim UrbSLaneKm(144, 0) As Double
    Dim FuelString As String
    Dim keylookup As String
    Dim newval As Double
    Dim stratcount As Long
    Dim PetOld(144, 0), PetNew, DieOld(144, 0), DieNew, EleOld(144, 0), EleNew, LPGOld(144, 0), LPGNew, CNGOld(144, 0), CNGNew, HydOld(144, 0), HydNew As Double
    Dim PetRat, DieRat, EleRat, LPGRat, CNGRat, HydRat As Double
    Dim VehCosts(4, 9) As Double
    Dim FuelCostPer(4, 9) As Double
    Dim VehFixedCosts(4, 9) As Double
    Dim VehFuelCosts(144, 4, 9) As Double
    Dim PHPerOld(144, 4), PHPerNew(4)
    Dim UrbRoadPer, WPPLTripPer As Double
    Dim CarbCharge(4, 9) As Double
    Dim newcapnum As Integer
    Dim zonecaparray(145, 8) As String
    Dim zonecapnum As Integer
    Dim enearray(91, 6) As String
    Dim RZEvInputArray(144, 35) As String
    Dim OutputArray(145, 46) As String


    Public Sub RoadZoneEVMain()


        'if using WPPL then check if the start year is a valid value
        If WPPL = True Then
            If WPPLYear < 2011 Then
                MsgBox("Invalid start year provided for WPPL.  Please rerun the model using a year between 2011 and 2100.")
                logarray(logNum, 0) = "Invalid start year provided for WPPL.  Run terminated during intrazonal road model external variable file generation."
                logNum += 1
                Call CloseLog()
                End
            ElseIf WPPLYear > 2100 Then
                MsgBox("Invalid start year provided for WPPL.  Please rerun the model using a year between 2011 and 2100.")
                logarray(logNum, 0) = "Invalid start year provided for WPPL.  Run terminated during intrazonal road model external variable file generation."
                logNum += 1
                Call CloseLog()
                End
            End If
        End If

        'get the input and output file names
        Call GetFiles()

        'if we are using a single scaling factor then set scaling factors - as a default they are just set to be constant over time
        If RdZPopSource = "Constant" Then
            PopGrowth = 1.005
        End If
        If RdZEcoSource = "Constant" Then
            GVAGrowth = 1.01
        End If
        If RdZEneSource = "Constant" Then
            CostGrowth = 1.01
        End If

        'read first line
        capnum = caparray(newcapnum, 8)
        zonecapcount = 0
        'transfer values to intermediate array
        Do Until capnum > RoadCapNum
            zonecapcount += 1
            For c = 0 To 8
                zonecapdetails(zonecapcount, c) = caparray(newcapnum, c)
            Next
            zonecapdetails(zonecapcount, 1) = RLCapYear(capnum)
            newcapnum += 1
            capnum = caparray(newcapnum, 8)
        Loop
        'then sort intermediate array by zone ID, then by year of implementation
        ReDim sortarray(zonecapcount - 1)
        For v = 0 To (zonecapcount - 1)
            padzone = String.Format("{0:000}", zonecapdetails(v, 0))
            padyear = String.Format("{0:00}", zonecapdetails(v, 1))
            sortarray(v) = padzone & "&" & padyear & "&" & v
        Next
        Array.Sort(sortarray)

        'reset the zonecaparray to the begining row
        zonecapnum = 1
        For v = 1 To (zonecapcount - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            zonecaparray(zonecapnum, 0) = g_modelRunID
            For c = 0 To 7
                zonecaparray(zonecapnum, c + 1) = zonecapdetails(arraynum, c)
            Next
            zonecapnum += 1
        Next
        'write all lines to intermediate capacity file
        Call WriteData("RoadZone", "RoadZoneCapChange", zonecaparray)

        'restart new cap array lines
        zonecapnum = 1
        Call GetCapData()

        If RdZEneSource = "Database" Then
            'get the values of the base year
            InputCount = 1
            Do While InputCount < 145
                PetOld(InputCount, 0) = enearray(1, 1)
                DieOld(InputCount, 0) = enearray(1, 2)
                EleOld(InputCount, 0) = enearray(1, 3)
                LPGOld(InputCount, 0) = enearray(1, 4)
                CNGOld(InputCount, 0) = enearray(1, 5)
                HydOld(InputCount, 0) = enearray(1, 6)
                InputCount += 1
            Loop

            'set base levels of fixed costs, and fuel costs will be set in the year 1 calculation of Sub CalcZoneData()
            'fixed costs
            VehFixedCosts(0, 0) = 0.7663 * 36.14
            VehFixedCosts(0, 1) = 0.7663 * 36.873
            VehFixedCosts(0, 2) = 0.7663 * 36.14
            VehFixedCosts(0, 3) = 0.7663 * 36.873
            For x = 4 To 9
                VehFixedCosts(0, x) = 0.7663 * 36.14
            Next
            For x = 0 To 9
                VehFixedCosts(1, x) = 0.845 * 61.329
            Next
            For x = 0 To 9
                VehFixedCosts(2, x) = 0.7791 * 93.665
            Next
            For x = 0 To 9
                VehFixedCosts(3, x) = 0.7065 * 109.948
            Next
            For x = 0 To 9
                VehFixedCosts(4, x) = 0.8699 * 234.5
            Next

        End If

        'Set year as 1 to start with
        Year = 1

        If WPPL = True Then
            WPPLStart = WPPLYear - 2010
        End If

        'loop through all years
        Do Until Year > 40

            InputCount = 1

            'calculate all 144 zones
            Do Until InputCount > 144
                Call CalcZoneData()
                InputCount += 1
            Loop

            'create the file if year 1
            'it is now writting to database, therefore no difference if it is year 1 or not
            If Year = 1 Then
                Call WriteData("RoadZone", "ExtVar", OutputArray, , True)
            Else
                Call WriteData("RoadZone", "ExtVar", OutputArray, , False)
            End If

            Year += 1
        Loop


    End Sub

    Sub GetFiles()

        'read initial input data
        Call ReadData("RoadZone", "Input", RZEvInputArray, g_modelRunYear)

        'read new capacity data
        Call ReadData("RoadZone", "NewCap", caparray, g_modelRunYear)
        newcapnum = 1

        'read energy file
        If RdZEneSource = "Database" Then
            Call ReadData("Energy", "", enearray, g_modelRunYear)
        End If

        'open the strategy file
        Call ReadData("SubStrategy", "", stratarray)

    End Sub
    'v1.6 to calculate by annual timesteps, parameters for each zone need to be seperated
    ' if zone number changes, the size of the array must also be changed
    ' unable to check if the code works for capyear, because the zone capacity does not changed during all 90 years

    Sub CalcZoneData()

        'read from initial file table if it is year 2011
        If Year = 1 Then
            ZoneID(InputCount, 0) = RZEvInputArray(InputCount, 4)
            PopOld(InputCount, 0) = get_population_data_by_zoneID(g_modelRunYear, ZoneID(InputCount, 0), "Zone", "'road'")
            GVAOld(InputCount, 0) = get_gva_data_by_zoneID(g_modelRunYear, Year + 2010, ZoneID(InputCount, 0), "Zone", "'road'")
            CostOld(InputCount, 0) = RZEvInputArray(InputCount, 8)
            LaneKm(InputCount, 0) = RZEvInputArray(InputCount, 9)
            MLaneKm(InputCount, 0) = RZEvInputArray(InputCount, 10)
            RurADLaneKm(InputCount, 0) = RZEvInputArray(InputCount, 11)
            RurASLaneKm(InputCount, 0) = RZEvInputArray(InputCount, 12)
            RurMinLaneKm(InputCount, 0) = RZEvInputArray(InputCount, 13)
            UrbDLaneKm(InputCount, 0) = RZEvInputArray(InputCount, 14)
            UrbSLaneKm(InputCount, 0) = RZEvInputArray(InputCount, 15)
            CostOld(InputCount, 1) = RZEvInputArray(InputCount, 20)
            CostOld(InputCount, 2) = RZEvInputArray(InputCount, 21)
            CostOld(InputCount, 3) = RZEvInputArray(InputCount, 22)
            CostOld(InputCount, 4) = RZEvInputArray(InputCount, 23)

            'v1.4 change set fuel efficiency old values to one
            For f = 0 To 34
                FuelEffOld(InputCount, f) = 1
            Next

            If RdZEneSource = "Database" Then
                'calculate base fuel costs
                VehFuelCosts(InputCount, 0, 0) = 0.2337 * 36.14
                VehFuelCosts(InputCount, 0, 1) = 0.1911 * 36.873
                VehFuelCosts(InputCount, 0, 2) = ((11.2 / 18.6) * 0.2337) / (0.7663 + ((11.2 / 18.6) * 0.2337)) * 36.14
                VehFuelCosts(InputCount, 0, 3) = ((7.6 / 12.4) * 0.1911) / (0.8089 + ((7.6 / 12.4) * 0.1911)) * 36.873
                'this and later plug-in hybrids is based on petrol/diesel being used for rural roads and electricity for urban roads
                VehFuelCosts(InputCount, 0, 4) = ((((18.1 / 25.9) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (46.7 / 25.9) * RZEvInputArray(InputCount, 31))) * 0.2337) / (0.7663 + ((((18.1 / 25.9) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (46.7 / 25.9) * RZEvInputArray(InputCount, 31))) * 0.2337)) * 36.14
                PHPerOld(InputCount, 0) = ((18.1 / 25.9) * (1 - RZEvInputArray(InputCount, 31))) / ((((18.1 / 25.9) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (46.7 / 25.9) * RZEvInputArray(InputCount, 31))))
                VehFuelCosts(InputCount, 0, 5) = ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (16.5 / 7.3) * 0.2337) / (0.7663 + ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (16.5 / 7.3) * 0.2337)) * 36.14
                VehFuelCosts(InputCount, 0, 8) = ((HydOld(InputCount, 0) / PetOld(InputCount, 0)) * (43.8 / 10.3) * 0.2337) / (0.7663 + ((HydOld(InputCount, 0) / PetOld(InputCount, 0)) * (43.8 / 10.3) * 0.2337)) * 36.14
                VehFuelCosts(InputCount, 0, 9) = ((HydOld(InputCount, 0) / PetOld(InputCount, 0)) * (53.3 / 25.9) * 0.2337) / (0.7663 + ((HydOld(InputCount, 0) / PetOld(InputCount, 0)) * (53.3 / 25.9) * 0.2337)) * 36.14
                VehFuelCosts(InputCount, 1, 0) = 0.155 * 61.329
                VehFuelCosts(InputCount, 1, 1) = 0.155 * 61.329
                VehFuelCosts(InputCount, 1, 3) = ((4.4 / 7.9) * 0.155) / (0.845 + ((4.4 / 7.9) * 0.155)) * 61.329
                VehFuelCosts(InputCount, 1, 4) = ((((5.8 / 7.9) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (42.3 / 7.9) * RZEvInputArray(InputCount, 31))) * 0.155) / (0.845 + ((((5.8 / 7.9) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (42.3 / 7.9) * RZEvInputArray(InputCount, 31))) * 0.155)) * 61.329
                PHPerOld(InputCount, 1) = ((5.8 / 7.9) * (1 - RZEvInputArray(InputCount, 31))) / ((((5.8 / 7.9) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (42.3 / 7.9) * RZEvInputArray(InputCount, 31))))
                VehFuelCosts(InputCount, 1, 5) = ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (56.2 / 7.9) * 0.155) / (0.845 + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (56.2 / 7.9) * 0.155)) * 61.329
                VehFuelCosts(InputCount, 1, 6) = ((LPGOld(InputCount, 0) / DieOld(InputCount, 0)) * (11.8 / 7.9) * 0.155) / (0.845 + ((LPGOld(InputCount, 0) / DieOld(InputCount, 0)) * (11.8 / 7.9) * 0.155)) * 61.329
                VehFuelCosts(InputCount, 1, 7) = ((CNGOld(InputCount, 0) / DieOld(InputCount, 0)) * (80.8 / 7.9) * 0.155) / (0.845 + ((CNGOld(InputCount, 0) / DieOld(InputCount, 0)) * (80.8 / 7.9) * 0.155)) * 61.329
                VehFuelCosts(InputCount, 2, 1) = 0.2209 * 93.665
                VehFuelCosts(InputCount, 2, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
                VehFuelCosts(InputCount, 2, 8) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (139.8 / 37.6) * 0.2209)) * 93.665
                VehFuelCosts(InputCount, 2, 9) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (112.3 / 37.6) * 0.2209)) * 93.665
                VehFuelCosts(InputCount, 3, 1) = 0.2935 * 109.948
                VehFuelCosts(InputCount, 3, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
                VehFuelCosts(InputCount, 3, 8) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (139.8 / 37.6) * 0.2935)) * 109.948
                VehFuelCosts(InputCount, 3, 9) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (112.3 / 37.6) * 0.2935)) * 109.948
                VehFuelCosts(InputCount, 4, 1) = 0.1301 * 234.5
                VehFuelCosts(InputCount, 4, 3) = ((30.4 / 37.2) * 0.1301) / (0.8699 + ((30.4 / 37.2) * 0.1301)) * 234.5
                VehFuelCosts(InputCount, 4, 4) = ((((11.9 / 19.6) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (103.7 / 19.6) * RZEvInputArray(InputCount, 31))) * 0.1301) / (0.8699 + ((((11.9 / 19.6) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (103.7 / 19.6) * RZEvInputArray(InputCount, 31))) * 0.1301)) * 234.5
                PHPerOld(InputCount, 4) = ((11.9 / 19.6) * (1 - RZEvInputArray(InputCount, 31))) / ((((11.9 / 19.6) * (1 - RZEvInputArray(InputCount, 31))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (103.7 / 19.6) * RZEvInputArray(InputCount, 31))))
                VehFuelCosts(InputCount, 4, 5) = ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (425.4 / 37.2) * 0.1301) / (0.8699 + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (425.4 / 37.2) * 0.1301)) * 234.5
                VehFuelCosts(InputCount, 4, 6) = ((LPGOld(InputCount, 0) / DieOld(InputCount, 0)) * (131.8 / 37.2) * 0.1301) / (0.8699 + ((LPGOld(InputCount, 0) / DieOld(InputCount, 0)) * (131.8 / 37.2) * 0.1301)) * 234.5
                VehFuelCosts(InputCount, 4, 7) = ((CNGOld(InputCount, 0) / DieOld(InputCount, 0)) * (1003.2 / 37.2) * 0.1301) / (0.8699 + ((CNGOld(InputCount, 0) / DieOld(InputCount, 0)) * (1003.2 / 37.2) * 0.1301)) * 234.5
                VehFuelCosts(InputCount, 4, 9) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (109.2 / 37.2) * 0.1301) / (0.8699 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (109.2 / 37.2) * 0.1301)) * 234.5

            End If

        End If



        'loop through scaling up values for each year and writing to output file until the 90th year

        If RdZPopSource = "Constant" Then
            PopNew = PopOld(InputCount, 0) * PopGrowth
        ElseIf RdZPopSource = "File" Then
            '***scaling files not currently set up for road zones module
        ElseIf RdZPopSource = "Database" Then
            'if year is after 2093 then no population forecasts are available so assume population remains constant
            'now modified as population data available up to 2100 - so should never need 'else'
            'v1.9 now read by using database function
            If Year < 91 Then
                PopNew = get_population_data_by_zoneID(g_modelRunYear, ZoneID(InputCount, 0), "Zone", "'road'")
            Else
                PopNew = PopOld(InputCount, 0)
            End If
        End If
        If RdZEcoSource = "Constant" Then
            GVANew = GVAOld(InputCount, 0) * GVAGrowth
        ElseIf RdZEcoSource = "File" Then
            '***scaling files not currently set up for road zones module
        ElseIf RdZEcoSource = "Database" Then
            'if year is after 2050 then no gva forecasts are available so assume gva remains constant
            'now modified as gva data available up to 2100 - so should never need 'else'
            'v1.9 now read by using database function
            'database does not have gva forecasts after year 2050, and the calculation is only available before year 2050
            If Year < 91 Then
                GVANew = get_gva_data_by_zoneID(g_modelRunYear, ZoneID(InputCount, 0), "Zone", "'road'")
            Else
                GVANew = GVAOld(InputCount, 0)
            End If
        End If

        'now amended to include different costs for different fuel types
        If RdZEneSource = "Database" Then

            PetNew = enearray(Year + 1, 1)
            DieNew = enearray(Year + 1, 2)
            EleNew = enearray(Year + 1, 3)
            LPGNew = enearray(Year + 1, 4)
            CNGNew = enearray(Year + 1, 5)
            HydNew = enearray(Year + 1, 6)
            'calculate ratio for each fuel
            PetRat = PetNew / PetOld(InputCount, 0)
            DieRat = DieNew / DieOld(InputCount, 0)
            EleRat = EleNew / EleOld(InputCount, 0)
            LPGRat = LPGNew / LPGOld(InputCount, 0)
            CNGRat = CNGNew / CNGOld(InputCount, 0)
            HydRat = HydNew / HydOld(InputCount, 0)
            'v1.4 change corrected fuel efficiency change calculation  - was previously just multiplying by figure straight from strategy array (which meant that fuel costs quickly declined to zero)
            For f = 0 To 34
                FuelEffNew(f) = stratarray(Year, f + 31)
                FuelEffChange(f) = FuelEffNew(f) / FuelEffOld(InputCount, f)
            Next
            'calculate cost for each vehicle type - 0 is car, 1 is LGV, 2 is small HGV, 3 is large HGV, 4 is PSV
            'calculate new cost for each fuel type within each vehicle type - 0 is petrol, 1 is diesel, 2 is petrol hybrid, 3 is diesel hybrid, 4 is plug-in hybrid, 5 is battery electric,
            '...6 is LPG, 7 is CNG, 8 is hydrogen IC, 9 is hydrogen fuel cell - by multiplying the fuel cost by the fuel ratio
            'the cost is also multiplied by changes in fuel efficiency
            VehFuelCosts(InputCount, 0, 0) = VehFuelCosts(InputCount, 0, 0) * PetRat * FuelEffChange(0)
            VehFuelCosts(InputCount, 0, 1) = VehFuelCosts(InputCount, 0, 1) * DieRat * FuelEffChange(1)
            VehFuelCosts(InputCount, 0, 2) = VehFuelCosts(InputCount, 0, 2) * PetRat * FuelEffChange(12)
            VehFuelCosts(InputCount, 0, 3) = VehFuelCosts(InputCount, 0, 3) * DieRat * FuelEffChange(13)
            PHPerNew(0) = (PHPerOld(InputCount, 0) * VehFuelCosts(InputCount, 0, 4) * PetRat) / ((PHPerOld(InputCount, 0) * VehFuelCosts(InputCount, 0, 4) * PetRat) + ((1 - PHPerOld(InputCount, 0)) * VehFuelCosts(InputCount, 0, 4) * EleRat))
            VehFuelCosts(InputCount, 0, 4) = ((PHPerOld(InputCount, 0) * VehFuelCosts(InputCount, 0, 4) * PetRat) + ((1 - PHPerOld(InputCount, 0)) * VehFuelCosts(InputCount, 0, 4) * EleRat)) * FuelEffChange(14)
            VehFuelCosts(InputCount, 0, 5) = VehFuelCosts(InputCount, 0, 5) * EleRat * FuelEffChange(2)
            VehFuelCosts(InputCount, 0, 8) = VehFuelCosts(InputCount, 0, 8) * HydRat * FuelEffChange(15)
            VehFuelCosts(InputCount, 0, 9) = VehFuelCosts(InputCount, 0, 9) * HydRat * FuelEffChange(16)
            VehFuelCosts(InputCount, 1, 0) = VehFuelCosts(InputCount, 1, 0) * PetRat * FuelEffChange(3)
            VehFuelCosts(InputCount, 1, 1) = VehFuelCosts(InputCount, 1, 1) * DieRat * FuelEffChange(4)
            VehFuelCosts(InputCount, 1, 3) = VehFuelCosts(InputCount, 1, 3) * DieRat * FuelEffChange(17)
            PHPerNew(1) = (PHPerOld(InputCount, 1) * VehFuelCosts(InputCount, 1, 4) * DieRat) / ((PHPerOld(InputCount, 1) * VehFuelCosts(InputCount, 1, 4) * DieRat) + ((1 - PHPerOld(InputCount, 1)) * VehFuelCosts(InputCount, 1, 4) * EleRat))
            VehFuelCosts(InputCount, 1, 4) = (PHPerOld(InputCount, 1) * VehFuelCosts(InputCount, 1, 4) * DieRat) + ((1 - PHPerOld(InputCount, 1)) * VehFuelCosts(InputCount, 1, 4) * EleRat) * FuelEffChange(18)
            VehFuelCosts(InputCount, 1, 5) = VehFuelCosts(InputCount, 1, 5) * EleRat * FuelEffChange(5)
            VehFuelCosts(InputCount, 1, 6) = VehFuelCosts(InputCount, 1, 6) * LPGRat * FuelEffChange(19)
            VehFuelCosts(InputCount, 1, 7) = VehFuelCosts(InputCount, 1, 7) * CNGRat * FuelEffChange(20)
            VehFuelCosts(InputCount, 2, 1) = VehFuelCosts(InputCount, 2, 1) * DieRat * FuelEffChange(6)
            VehFuelCosts(InputCount, 2, 3) = VehFuelCosts(InputCount, 2, 3) * DieRat * FuelEffChange(26)
            VehFuelCosts(InputCount, 2, 8) = VehFuelCosts(InputCount, 2, 8) * HydRat * FuelEffChange(27)
            VehFuelCosts(InputCount, 2, 9) = VehFuelCosts(InputCount, 2, 9) * HydRat * FuelEffChange(28)
            VehFuelCosts(InputCount, 3, 1) = VehFuelCosts(InputCount, 3, 1) * DieRat * FuelEffChange(8)
            VehFuelCosts(InputCount, 3, 3) = VehFuelCosts(InputCount, 3, 3) * DieRat * FuelEffChange(29)
            VehFuelCosts(InputCount, 3, 8) = VehFuelCosts(InputCount, 3, 8) * HydRat * FuelEffChange(31)
            VehFuelCosts(InputCount, 3, 9) = VehFuelCosts(InputCount, 3, 9) * HydRat * FuelEffChange(32)
            VehFuelCosts(InputCount, 4, 1) = VehFuelCosts(InputCount, 4, 1) * DieRat * FuelEffChange(10)
            VehFuelCosts(InputCount, 4, 3) = VehFuelCosts(InputCount, 4, 3) * DieRat * FuelEffChange(21)
            PHPerNew(4) = (PHPerOld(InputCount, 4) * VehFuelCosts(InputCount, 4, 4) * DieRat) / ((PHPerOld(InputCount, 4) * VehFuelCosts(InputCount, 4, 4) * DieRat) + ((1 - PHPerOld(InputCount, 4)) * VehFuelCosts(InputCount, 4, 4) * EleRat))
            VehFuelCosts(InputCount, 4, 4) = (PHPerOld(InputCount, 4) * VehFuelCosts(InputCount, 4, 4) * DieRat) + ((1 - PHPerOld(InputCount, 4)) * VehFuelCosts(InputCount, 4, 4) * EleRat) * FuelEffChange(22)
            VehFuelCosts(InputCount, 4, 5) = VehFuelCosts(InputCount, 4, 5) * EleRat * FuelEffChange(11)
            VehFuelCosts(InputCount, 4, 6) = VehFuelCosts(InputCount, 4, 6) * LPGRat * FuelEffChange(23)
            VehFuelCosts(InputCount, 4, 7) = VehFuelCosts(InputCount, 4, 7) * CNGRat * FuelEffChange(24)
            VehFuelCosts(InputCount, 4, 9) = VehFuelCosts(InputCount, 4, 9) * HydRat * FuelEffChange(25)
            'v1.3 if using carbon charge then need to add that, assuming it is after the year of introduction
            If CarbonCharge = True Then
                If Year >= CarbChargeYear Then
                    'note that we assume base (2010) petrol price of 122.1 p/litre when calculating the base fuel consumption (full calculations from base figures not included in model run)
                    'calculation is: (base fuel units per km * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
                    CarbCharge(0, 0) = (0.086 * stratarray(Year, 31) * stratarray(Year, 72) * (stratarray(Year, 71) / 10))
                    CarbCharge(0, 1) = (0.057 * stratarray(Year, 32) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(0, 2) = (0.056 * stratarray(Year, 43) * stratarray(Year, 72) * (stratarray(Year, 71) / 10))
                    CarbCharge(0, 3) = (0.038 * stratarray(Year, 44) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(0, 4) = (PHPerOld(InputCount, 0) * (0.06 * stratarray(Year, 45) * stratarray(Year, 72) * (stratarray(Year, 71) / 10))) + ((1 - PHPerOld(InputCount, 0)) * (0.016 * stratarray(Year, 45) * stratarray(Year, 72) * (stratarray(Year, 70) / 10)))
                    CarbCharge(0, 5) = (0.165 * stratarray(Year, 33) * stratarray(Year, 74) * (stratarray(Year, 70) / 10))
                    CarbCharge(0, 8) = (0.438 * stratarray(Year, 46) * stratarray(Year, 77) * (stratarray(Year, 71) / 10))
                    CarbCharge(0, 9) = (0.178 * stratarray(Year, 47) * stratarray(Year, 77) * (stratarray(Year, 71) / 10))
                    CarbCharge(1, 0) = (0.088 * stratarray(Year, 34) * stratarray(Year, 72) * (stratarray(Year, 71) / 10))
                    CarbCharge(1, 1) = (0.079 * stratarray(Year, 35) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(1, 3) = (0.044 * stratarray(Year, 48) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(1, 4) = (PHPerOld(InputCount, 1) * (0.058 * stratarray(Year, 49) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))) + ((1 - PHPerOld(InputCount, 1)) * (0.423 * stratarray(Year, 49) * stratarray(Year, 73) * (stratarray(Year, 70) / 10)))
                    CarbCharge(1, 5) = (0.562 * stratarray(Year, 36) * stratarray(Year, 74) * (stratarray(Year, 70) / 10))
                    CarbCharge(1, 6) = (0.118 * stratarray(Year, 50) * stratarray(Year, 75) * (stratarray(Year, 71) / 10))
                    CarbCharge(1, 7) = (0.808 * stratarray(Year, 51) * stratarray(Year, 76) * (stratarray(Year, 71) / 10))
                    CarbCharge(2, 1) = (0.259 * stratarray(Year, 37) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(2, 3) = (0.15 * stratarray(Year, 57) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(2, 8) = (0.957 * stratarray(Year, 58) * stratarray(Year, 77) * (stratarray(Year, 71) / 10))
                    CarbCharge(2, 9) = (0.898 * stratarray(Year, 59) * stratarray(Year, 77) * (stratarray(Year, 71) / 10))
                    CarbCharge(3, 1) = (0.376 * stratarray(Year, 39) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(3, 3) = (0.221 * stratarray(Year, 60) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(3, 8) = (1.398 * stratarray(Year, 61) * stratarray(Year, 77) * (stratarray(Year, 71) / 10))
                    CarbCharge(3, 9) = (1.123 * stratarray(Year, 62) * stratarray(Year, 77) * (stratarray(Year, 71) / 10))
                    CarbCharge(4, 1) = (0.176 * stratarray(Year, 41) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(4, 3) = (0.185 * stratarray(Year, 52) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))
                    CarbCharge(4, 4) = (PHPerOld(InputCount, 4) * (0.119 * stratarray(Year, 53) * stratarray(Year, 73) * (stratarray(Year, 71) / 10))) + ((1 - PHPerOld(InputCount, 4)) * (1.037 * stratarray(Year, 53) * stratarray(Year, 73) * (stratarray(Year, 70) / 10)))
                    CarbCharge(4, 5) = (0.2554 * stratarray(Year, 42) * stratarray(Year, 74) * (stratarray(Year, 70) / 10))
                    CarbCharge(4, 6) = (0.954 * stratarray(Year, 54) * stratarray(Year, 75) * (stratarray(Year, 71) / 10))
                    CarbCharge(4, 7) = (3.749 * stratarray(Year, 55) * stratarray(Year, 76) * (stratarray(Year, 71) / 10))
                    CarbCharge(4, 9) = (0.546 * stratarray(Year, 56) * stratarray(Year, 77) * (stratarray(Year, 71) / 10))
                End If
            End If
            'add the fixed costs
            'v1.3 and also add the carbon charge if we are using one
            If CarbonCharge = True Then
                For x = 0 To 4
                    For y = 0 To 9
                        VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(InputCount, x, y) + CarbCharge(x, y)
                    Next
                Next
            Else
                For x = 0 To 4
                    For y = 0 To 9
                        VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(InputCount, x, y)
                    Next
                Next
            End If
            'then multiply these costs by the proportions of vehicles in each fuel type (from strategy file), and aggregate the cost for each vehicle type
            CostNew(0) = (VehCosts(0, 0) * stratarray(Year, 1)) + (VehCosts(0, 1) * stratarray(Year, 2)) + (VehCosts(0, 2) * stratarray(Year, 14)) + (VehCosts(0, 3) * stratarray(Year, 15)) + (VehCosts(0, 4) * stratarray(Year, 16)) + (VehCosts(0, 5) * stratarray(Year, 3)) + (VehCosts(0, 8) * stratarray(Year, 17)) + (VehCosts(0, 9) * stratarray(Year, 18))
            CostNew(1) = (VehCosts(1, 0) * stratarray(Year, 4)) + (VehCosts(1, 1) * stratarray(Year, 5)) + (VehCosts(1, 3) * stratarray(Year, 19)) + (VehCosts(1, 4) * stratarray(Year, 20)) + (VehCosts(1, 5) * stratarray(Year, 6)) + (VehCosts(1, 6) * stratarray(Year, 21)) + (VehCosts(1, 7) * stratarray(Year, 22))
            CostNew(2) = (VehCosts(2, 1) * stratarray(Year, 7)) + (VehCosts(2, 3) * stratarray(Year, 28)) + (VehCosts(2, 8) * stratarray(Year, 29)) + (VehCosts(2, 9) * stratarray(Year, 30))
            CostNew(3) = (VehCosts(3, 1) * stratarray(Year, 7)) + (VehCosts(3, 3) * stratarray(Year, 28)) + (VehCosts(3, 8) * stratarray(Year, 29)) + (VehCosts(3, 9) * stratarray(Year, 30))
            CostNew(4) = (VehCosts(4, 1) * stratarray(Year, 9)) + (VehCosts(4, 3) * stratarray(Year, 23)) + (VehCosts(4, 4) * stratarray(Year, 24)) + (VehCosts(4, 5) * stratarray(Year, 10)) + (VehCosts(4, 6) * stratarray(Year, 25)) + (VehCosts(4, 7) * stratarray(Year, 26)) + (VehCosts(4, 9) * stratarray(Year, 27))
        Else
            For x = 0 To 4
                CostNew(x) = CostOld(InputCount, x) * CostGrowth
            Next
        End If

        'if including capacity changes, then check if there are any capacity changes for this zone

        If ZoneID(InputCount, 0) = CapID Then
            'if there are any capacity changes for this zone, check if there are any capacity changes for this year
            If Year = CapYear Then
                'if there are, then update the capacity variables, and read in the next row from the capacity file
                MLaneKm(InputCount, 0) += MwayKmChange
                RurADLaneKm(InputCount, 0) += RurADKmChange
                RurASLaneKm(InputCount, 0) += RurASKmChange
                RurMinLaneKm(InputCount, 0) += RurMinKmChange
                UrbDLaneKm(InputCount, 0) += UrbDKmChange
                UrbSLaneKm(InputCount, 0) += UrbSKmChange
                LaneKm(InputCount, 0) = MLaneKm(InputCount, 0) + RurADLaneKm(InputCount, 0) + RurASLaneKm(InputCount, 0) + RurMinLaneKm(InputCount, 0) + UrbDLaneKm(InputCount, 0) + UrbSLaneKm(InputCount, 0)
                Call GetCapData()
            End If
        End If
        'add in workplace parking levy if necessary
        If WPPL = True Then
            If Year >= WPPLStart Then
                UrbRoadPer = (UrbDLaneKm(InputCount, 0) + UrbSLaneKm(InputCount, 0)) / LaneKm(InputCount, 0)
                'levy only applies to 20% of trips on urban roads
                WPPLTripPer = 0.2 * UrbRoadPer
                CostNew(0) = ((1 - WPPLTripPer) * CostNew(0)) + (WPPLTripPer * (CostNew(0) * (1 + (WPPLPer / 100))))
            End If
        End If
        'define fuel split - this is now specified via the strategy common variables file
        'FuelString = "0.598,0.402,0,0.055,0.945,0,1,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,"
        'write to output file
        OutputArray(InputCount, 0) = g_modelRunID
        OutputArray(InputCount, 1) = ZoneID(InputCount, 0)
        OutputArray(InputCount, 2) = Year
        OutputArray(InputCount, 3) = PopNew
        OutputArray(InputCount, 4) = GVANew
        OutputArray(InputCount, 5) = CostNew(0)
        OutputArray(InputCount, 6) = LaneKm(InputCount, 0)
        OutputArray(InputCount, 7) = MLaneKm(InputCount, 0)
        OutputArray(InputCount, 8) = RurADLaneKm(InputCount, 0)
        OutputArray(InputCount, 9) = RurASLaneKm(InputCount, 0)
        OutputArray(InputCount, 10) = RurMinLaneKm(InputCount, 0)
        OutputArray(InputCount, 11) = UrbDLaneKm(InputCount, 0)
        OutputArray(InputCount, 12) = UrbSLaneKm(InputCount, 0)
        'fuel split now comes from the strategy file
        'build the fuel string from the strategy file row
        stratcount = 1
        Do While stratcount < 31
            OutputArray(InputCount, 12 + stratcount) = stratarray(Year, stratcount)
            stratcount += 1
        Loop
        OutputArray(InputCount, 43) = CostNew(1)
        OutputArray(InputCount, 44) = CostNew(2)
        OutputArray(InputCount, 45) = CostNew(3)
        OutputArray(InputCount, 46) = CostNew(4)
        'set old values as previous new values
        PopOld(InputCount, 0) = PopNew
        GVAOld(InputCount, 0) = GVANew
        PetOld(InputCount, 0) = PetNew
        DieOld(InputCount, 0) = DieNew
        EleOld(InputCount, 0) = EleNew
        LPGOld(InputCount, 0) = LPGNew
        CNGOld(InputCount, 0) = CNGNew
        HydOld(InputCount, 0) = HydNew
        For x = 0 To 4
            CostOld(InputCount, x) = CostNew(x)
            PHPerOld(InputCount, x) = PHPerNew(x)
        Next
        'v1.4 change
        For f = 0 To 34
            FuelEffOld(InputCount, f) = FuelEffNew(f)
        Next



    End Sub

    Sub GetCapData()

        If zonecaparray(zonecapnum, 0) Is Nothing Then
            'do nothing if reach the end
        Else
            CapID = zonecaparray(zonecapnum, 0)
            CapYear = zonecaparray(zonecapnum, 1)
            MwayKmChange = zonecaparray(zonecapnum, 2)
            RurADKmChange = zonecaparray(zonecapnum, 3)
            RurASKmChange = zonecaparray(zonecapnum, 4)
            RurMinKmChange = zonecaparray(zonecapnum, 5)
            UrbDKmChange = zonecaparray(zonecapnum, 6)
            UrbSKmChange = zonecaparray(zonecapnum, 7)
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
