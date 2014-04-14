Module UpdateEVs
    'this works for all modules

    Dim OldExtVarFile As IO.FileStream
    Dim oev As IO.StreamReader
    Dim NewExtVarFile As IO.FileStream
    Dim nev As IO.StreamWriter
    Dim CapFile As IO.FileStream
    Dim cpf As IO.StreamReader
    Dim FileNameTxt As String
    Dim CapNameTxt As String
    Dim ModelArea As String
    Dim CapChangeRow As String
    Dim CapChangeDetails() As String
    Dim NewMLanes As Integer
    Dim NewDLanes As Integer
    Dim NewSLanes As Integer
    Dim OldMLanes, OldDLanes, OldSLanes As Integer
    Dim EVLine As String
    Dim EVDetails() As String
    Dim FlowUpdated As Boolean
    Dim FlowChanged As Long
    Dim ChangeYear As Long
    Dim CurrentYear As Long
    Dim NewZoneLKm, OldZMLKm, NewZMLKm, OldZRADLKm, NewZRADLKm, OldZRASLKm, NewZRASLKm, OldZRMLKm, NewZRMLKm, OldZUDLKm, NewZUDLKm, OldZUSLKm, NewZUSLKm As Double
    Dim OldTracks, NewTracks As Integer
    Dim OldMaxTD, NewMaxTD As Double
    Dim OldStations, NewStations As Integer
    Dim NewTrips As Double
    Dim BaseTermCap, BaseATM As Long
    Dim ATMChange(29) As Long
    Dim TermChange(29) As Long
    Dim OldLBCap, OldDBCap, OldGCCap, OldLLCap, OldRRCap, NewLBCap, NewDBCap, NewGCCap, NewLLCap, NewRRCap As Double

    Public Sub UpdateExtVarFiles()
        EVFileSuffix = "Updated"
        'check if updating road link file
        If NewRdLCap = True Then
            'if so then update this file
            ModelArea = "RoadLink"
            Call UpdateRdLEV()
        End If
        'check if updating road zone file
        If NewRdZCap = True Then
            'if so then update this file
            ModelArea = "RoadZone"
            Call UpdateRdZEV()
        End If
        'check if updating rail link file
        If NewRlLCap = True Then
            'if so then update this file
            ModelArea = "RailLink"
            Call UpdateRlLEV()
        End If
        'check if updating rail zone file
        If NewRlZCap = True Then
            'if so then update this file
            ModelArea = "RailZone"
            Call UpdateRlZEV()
        End If
        'check if updating airport file
        If NewAirCap = True Then
            'if so then update this file
            ModelArea = "Airport"
            Call UpdateAirEV()
        End If
        'check if updating seaport file
        If NewSeaCap = True Then
            'if so then update this file
            ModelArea = "Seaport"
            Call UpdateSeaEV()
        End If

    End Sub

    Sub UpdateRdLEV()

        Dim ChangeFlow As Long
        Dim CurrentFlow As Long

        FileNameTxt = "ExternalVariables"
        CapNameTxt = "RoadLink"
        Call GetEVFiles()
        'read first line from capacity change file
        CapChangeRow = cpf.ReadLine()
        FlowUpdated = False
        'read first line from EV file
        EVLine = oev.ReadLine
        'this is a dummy try loop to allow us to break out of all the do loops when we get to the end of the file
        Try
            'loop through rows in capacity change file until all have been read
            Do Until EVLine Is Nothing
                'if there are other capacity changes to deal with then split row into array
                If CapChangeRow Is Nothing Then
                Else
                    CapChangeDetails = Split(CapChangeRow, ",")
                    ChangeFlow = CapChangeDetails(0)
                    ChangeYear = CapChangeDetails(1)
                    ChangeYear = ChangeYear - 2010
                End If
                'split EV line into array
                EVDetails = Split(EVLine, ",")
                CurrentFlow = EVDetails(0)
                CurrentYear = EVDetails(1)
                'if capacity has been altered for a flow then check if we're still on the same flow
                If FlowUpdated = True Then
                    If CurrentFlow = FlowChanged Then
                    Else
                        FlowUpdated = False
                    End If
                End If
                If CurrentFlow = ChangeFlow Then
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        FlowUpdated = True
                        FlowChanged = CurrentFlow
                        'alter the EV details for that flow
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentFlow = EVDetails(0)
                            CurrentYear = EVDetails(1)
                        Loop
                        'once at the correct year then alter the EV details for that flow
                        FlowUpdated = True
                        FlowChanged = CurrentFlow
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                Else
                    'otherwise, write line to output file and loop until we get to the correct flow
                    Do Until CurrentFlow = ChangeFlow
                        Call WriteOutputLine()
                        EVLine = oev.ReadLine
                        If EVLine Is Nothing Then
                            Exit Try
                        End If
                        EVDetails = Split(EVLine, ",")
                        CurrentFlow = EVDetails(0)
                        CurrentYear = EVDetails(1)
                        'if capacity has been altered for a flow then check if we're still on the same flow
                        If FlowUpdated = True Then
                            If CurrentFlow = FlowChanged Then
                            Else
                                FlowUpdated = False
                            End If
                        End If
                    Loop
                    'when we do get to that flow then alter the EVDetails for that flow
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        'alter the EV details for that flow
                        FlowUpdated = True
                        FlowChanged = CurrentFlow
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentFlow = EVDetails(0)
                            CurrentYear = EVDetails(1)
                        Loop
                        'once at the correct year then alter the EV details for that flow
                        FlowUpdated = True
                        FlowChanged = CurrentFlow
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                End If
                CapChangeRow = cpf.ReadLine()
                EVLine = oev.ReadLine
            Loop
        Finally
        End Try

        oev.Close()
        cpf.Close()
        nev.Close()
    End Sub

    Sub UpdateRdZEV()

        Dim ChangeZone As Long
        Dim CurrentZone As Long

        FileNameTxt = "RoadZoneExtVar"
        CapNameTxt = "RoadZone"
        Call GetEVFiles()
        'read first line from capacity change file
        CapChangeRow = cpf.ReadLine()
        FlowUpdated = False
        'read first line from EV file
        EVLine = oev.ReadLine
        'this is a dummy try loop to allow us to break out of all the do loops when we get to the end of the file
        Try
            'loop through rows in capacity change file until all have been read
            Do Until EVLine Is Nothing
                'if there are other capacity changes to deal with then split row into array
                If CapChangeRow Is Nothing Then
                Else
                    CapChangeDetails = Split(CapChangeRow, ",")
                    ChangeZone = CapChangeDetails(0)
                    ChangeYear = CapChangeDetails(1)
                    ChangeYear = ChangeYear - 2010
                End If
                'split EV line into array
                EVDetails = Split(EVLine, ",")
                CurrentZone = EVDetails(0)
                CurrentYear = EVDetails(1)
                'if capacity has been altered for a flow then check if we're still on the same flow
                If FlowUpdated = True Then
                    If CurrentZone = FlowChanged Then
                    Else
                        FlowUpdated = False
                    End If
                End If
                If CurrentZone = ChangeZone Then
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        'alter the EV details for that flow
                        FlowChanged = CurrentZone
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentZone = EVDetails(0)
                            CurrentYear = EVDetails(1)
                        Loop
                        'once at the correct year then alter the EV details for that flow
                        FlowChanged = CurrentZone
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                Else
                    'otherwise, write line to output file and loop until we get to the correct flow
                    Do Until CurrentZone = ChangeZone
                        Call WriteOutputLine()
                        EVLine = oev.ReadLine
                        If EVLine Is Nothing Then
                            Exit Try
                        End If
                        EVDetails = Split(EVLine, ",")
                        CurrentZone = EVDetails(0)
                        CurrentYear = EVDetails(1)
                        'if capacity has been altered for a flow then check if we're still on the same flow
                        If FlowUpdated = True Then
                            If CurrentZone = FlowChanged Then
                            Else
                                FlowUpdated = False
                            End If
                        End If
                    Loop
                    'when we do get to that flow then alter the EVDetails for that flow
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        'alter the EV details for that flow
                        FlowChanged = CurrentZone
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentZone = EVDetails(0)
                            CurrentYear = EVDetails(1)
                        Loop
                        'once at the correct year then alter the EV details for that flow
                        FlowChanged = CurrentZone
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                End If
                CapChangeRow = cpf.ReadLine()
                EVLine = oev.ReadLine
            Loop
        Finally
        End Try

        oev.Close()
        cpf.Close()
        nev.Close()

    End Sub

    Sub UpdateRlLEV()

        Dim ChangeFlow As Long
        Dim CurrentFlow As Long

        FileNameTxt = "RailLinkExtVar"
        CapNameTxt = "RailLink"
        Call GetEVFiles()
        'read first line from capacity change file
        CapChangeRow = cpf.ReadLine()
        FlowUpdated = False
        'read first line from EV file
        EVLine = oev.ReadLine
        'this is a dummy try loop to allow us to break out of all the do loops when we get to the end of the file
        Try
            'loop through rows in capacity change file until all have been read
            Do Until EVLine Is Nothing
                'if there are other capacity changes to deal with then split row into array
                If CapChangeRow Is Nothing Then
                Else
                    CapChangeDetails = Split(CapChangeRow, ",")
                    ChangeFlow = CapChangeDetails(0)
                    ChangeYear = CapChangeDetails(1)
                    ChangeYear = ChangeYear - 2010
                End If
                'split EV line into array
                EVDetails = Split(EVLine, ",")
                CurrentFlow = EVDetails(0)
                CurrentYear = EVDetails(1)
                'if capacity has been altered for a flow then check if we're still on the same flow
                If FlowUpdated = True Then
                    If CurrentFlow = FlowChanged Then
                    Else
                        FlowUpdated = False
                    End If
                End If
                If CurrentFlow = ChangeFlow Then
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        FlowUpdated = True
                        FlowChanged = CurrentFlow
                        'alter the EV details for that flow
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentFlow = EVDetails(0)
                            CurrentYear = EVDetails(1)
                        Loop
                        'once at the correct year then alter the EV details for that flow
                        FlowUpdated = True
                        FlowChanged = CurrentFlow
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                Else
                    'otherwise, write line to output file and loop until we get to the correct flow
                    Do Until CurrentFlow = ChangeFlow
                        Call WriteOutputLine()
                        EVLine = oev.ReadLine
                        If EVLine Is Nothing Then
                            Exit Try
                        End If
                        EVDetails = Split(EVLine, ",")
                        CurrentFlow = EVDetails(0)
                        CurrentYear = EVDetails(1)
                        'if capacity has been altered for a flow then check if we're still on the same flow
                        If FlowUpdated = True Then
                            If CurrentFlow = FlowChanged Then
                            Else
                                FlowUpdated = False
                            End If
                        End If
                    Loop
                    'when we do get to that flow then alter the EVDetails for that flow
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        'alter the EV details for that flow
                        FlowUpdated = True
                        FlowChanged = CurrentFlow
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentFlow = EVDetails(0)
                            CurrentYear = EVDetails(1)
                        Loop
                        'once at the correct year then alter the EV details for that flow
                        FlowUpdated = True
                        FlowChanged = CurrentFlow
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                End If
                CapChangeRow = cpf.ReadLine()
                EVLine = oev.ReadLine
            Loop
        Finally
        End Try

        oev.Close()
        cpf.Close()
        nev.Close()
    End Sub

    Sub UpdateRlZEV()

        Dim ChangeZone As Long
        Dim CurrentZone As Long

        FileNameTxt = "RailZoneExtVar"
        CapNameTxt = "RailZone"
        Call GetEVFiles()
        'read first line from capacity change file
        CapChangeRow = cpf.ReadLine()
        FlowUpdated = False
        'read first line from EV file
        EVLine = oev.ReadLine
        'this is a dummy try loop to allow us to break out of all the do loops when we get to the end of the file
        Try
            'loop through rows in capacity change file until all have been read
            Do Until EVLine Is Nothing
                'if there are other capacity changes to deal with then split row into array
                If CapChangeRow Is Nothing Then
                Else
                    CapChangeDetails = Split(CapChangeRow, ",")
                    ChangeZone = CapChangeDetails(0)
                    ChangeYear = CapChangeDetails(1)
                    ChangeYear = ChangeYear - 2010
                End If
                'split EV line into array
                EVDetails = Split(EVLine, ",")
                CurrentZone = EVDetails(0)
                CurrentYear = EVDetails(1)
                'if capacity has been altered for a zone then check if we're still on the same zone
                If FlowUpdated = True Then
                    If CurrentZone = FlowChanged Then
                    Else
                        FlowUpdated = False
                    End If
                End If
                If CurrentZone = ChangeZone Then
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        'alter the EV details for that flow
                        FlowChanged = CurrentZone
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentZone = EVDetails(0)
                            CurrentYear = EVDetails(1)
                        Loop
                        'once at the correct year then alter the EV details for that zone
                        FlowChanged = CurrentZone
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                Else
                    'otherwise, write line to output file and loop until we get to the correct zone
                    Do Until CurrentZone = ChangeZone
                        Call WriteOutputLine()
                        EVLine = oev.ReadLine
                        If EVLine Is Nothing Then
                            Exit Try
                        End If
                        EVDetails = Split(EVLine, ",")
                        CurrentZone = EVDetails(0)
                        CurrentYear = EVDetails(1)
                        'if capacity has been altered for a zone then check if we're still on the same zone
                        If FlowUpdated = True Then
                            If CurrentZone = FlowChanged Then
                            Else
                                FlowUpdated = False
                            End If
                        End If
                    Loop
                    'when we do get to that zone then alter the EVDetails for that zone
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        'alter the EV details for that zone
                        FlowChanged = CurrentZone
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentZone = EVDetails(0)
                            CurrentYear = EVDetails(1)
                        Loop
                        'once at the correct year then alter the EV details for that zone
                        FlowChanged = CurrentZone
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                End If
                CapChangeRow = cpf.ReadLine()
                EVLine = oev.ReadLine
            Loop
        Finally
        End Try

        oev.Close()
        cpf.Close()
        nev.Close()
    End Sub

    Sub UpdateAirEV()

        Dim ChangeNode As Long
        Dim CurrentNode As Long
        Dim TermAddCap As Long
        Dim ATMAddCap As Long

        FileNameTxt = "AirNodeExtVar"
        CapNameTxt = "AirNode"
        'set flow updated variable to true because this affects the write output line sub - otherwise this variable is not relevant to air
        FlowUpdated = True
        Call GetEVFiles()
        'read first line from capacity change file
        CapChangeRow = cpf.ReadLine()
        'split row into array
        If CapChangeRow Is Nothing Then
        Else
            CapChangeDetails = Split(CapChangeRow, ",")
            ChangeNode = CapChangeDetails(0)
            ChangeYear = CapChangeDetails(1)
            ChangeYear = ChangeYear - 2010
        End If
        'read first line from EV file
        EVLine = oev.ReadLine
        'this is a dummy try loop to allow us to break out of all the do loops when we get to the end of the file
        Try
            'loop through rows in capacity change file until all have been read
            Do Until EVLine Is Nothing
                'split EV line into array
                EVDetails = Split(EVLine, ",")
                CurrentYear = EVDetails(0)
                CurrentNode = EVDetails(1)
                'set base capacity (which in this case is reset every time a new line is read from the original external variables file)
                Call SetBaseCapacity()
                'check if we're in a year when capacity has been altered
                If CurrentYear = ChangeYear Then
                    'if we are then check if we are at the correct airport
                    If CurrentNode = ChangeNode Then
                        'if we are then update the additional capacity variables, write output row and read additional line from capacity file
                        TermAddCap = CapChangeDetails(2)
                        ATMAddCap = CapChangeDetails(3)
                        TermChange(CurrentNode) += TermAddCap
                        ATMChange(CurrentNode) += ATMAddCap
                        Call WriteOutputLine()
                        'read row from capacity file
                        CapChangeRow = cpf.ReadLine()
                        'split row into array
                        If CapChangeRow Is Nothing Then
                        Else
                            CapChangeDetails = Split(CapChangeRow, ",")
                            ChangeNode = CapChangeDetails(0)
                            ChangeYear = CapChangeDetails(1)
                            ChangeYear = ChangeYear - 2010
                        End If
                    Else
                        'if not then write output row
                        Call WriteOutputLine()
                    End If
                Else
                    'if not then write output row
                    Call WriteOutputLine()
                End If
                EVLine = oev.ReadLine
            Loop
        Finally
        End Try

        oev.Close()
        cpf.Close()
        nev.Close()
    End Sub

    Sub UpdateSeaEV()
        Dim ChangePort As Long
        Dim CurrentPort As Long

        FileNameTxt = "SeaFreightExtVar"
        CapNameTxt = "SeaFreight"
        Call GetEVFiles()
        'read first line from capacity change file
        CapChangeRow = cpf.ReadLine()
        FlowUpdated = False
        'read first line from EV file
        EVLine = oev.ReadLine
        'this is a dummy try loop to allow us to break out of all the do loops when we get to the end of the file
        Try
            'loop through rows in capacity change file until all have been read
            Do Until EVLine Is Nothing
                'if there are other capacity changes to deal with then split row into array
                If CapChangeRow Is Nothing Then
                Else
                    CapChangeDetails = Split(CapChangeRow, ",")
                    ChangePort = CapChangeDetails(0)
                    ChangeYear = CapChangeDetails(1)
                    ChangeYear = ChangeYear - 2010
                End If
                'split EV line into array
                EVDetails = Split(EVLine, ",")
                CurrentPort = EVDetails(1)
                CurrentYear = EVDetails(0)
                'if capacity has been altered for a flow then check if we're still on the same flow
                If FlowUpdated = True Then
                    If CurrentPort = FlowChanged Then
                    Else
                        FlowUpdated = False
                    End If
                End If
                If CurrentPort = ChangePort Then
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        'alter the EV details for that flow
                        FlowChanged = CurrentPort
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentPort = EVDetails(1)
                            CurrentYear = EVDetails(0)
                        Loop
                        'once at the correct year then alter the EV details for that flow
                        FlowChanged = CurrentPort
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                Else
                    'otherwise, write line to output file and loop until we get to the correct flow
                    Do Until CurrentPort = ChangePort
                        Call WriteOutputLine()
                        EVLine = oev.ReadLine
                        If EVLine Is Nothing Then
                            Exit Try
                        End If
                        EVDetails = Split(EVLine, ",")
                        CurrentPort = EVDetails(1)
                        CurrentYear = EVDetails(0)
                        'if capacity has been altered for a flow then check if we're still on the same flow
                        If FlowUpdated = True Then
                            If CurrentPort = FlowChanged Then
                            Else
                                FlowUpdated = False
                            End If
                        End If
                    Loop
                    'when we do get to that flow then alter the EVDetails for that flow
                    If FlowUpdated = False Then
                        Call SetBaseCapacity()
                    End If
                    'see if we are in the correct year
                    If CurrentYear = ChangeYear Then
                        'alter the EV details for that flow
                        FlowChanged = CurrentPort
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    Else
                        'otherwise, write line to output file and loop until we get to the correct year
                        Do Until CurrentYear = ChangeYear
                            Call WriteOutputLine()
                            EVLine = oev.ReadLine
                            If EVLine Is Nothing Then
                                Exit Try
                            End If
                            EVDetails = Split(EVLine, ",")
                            CurrentPort = EVDetails(1)
                            CurrentYear = EVDetails(0)
                        Loop
                        'once at the correct year then alter the EV details for that flow
                        FlowChanged = CurrentPort
                        FlowUpdated = True
                        Call UpdateEVLine()
                        'write line to output file
                        Call WriteOutputLine()
                    End If
                End If
                CapChangeRow = cpf.ReadLine()
                EVLine = oev.ReadLine
            Loop
        Finally
        End Try

        oev.Close()
        cpf.Close()
        nev.Close()
    End Sub

    Sub GetEVFiles()
        Dim row As String

        'get old external variable file
        OldExtVarFile = New IO.FileStream(DirPath & EVFilePrefix & FileNameTxt & ".csv", IO.FileMode.Open)
        oev = New IO.StreamReader(OldExtVarFile, System.Text.Encoding.Default)
        'read header row
        row = oev.ReadLine

        'get capacity file
        CapFile = New IO.FileStream(DirPath & CapNameTxt & "CapChange.csv", IO.FileMode.Open)
        cpf = New IO.StreamReader(CapFile, System.Text.Encoding.Default)
        'read header row
        row = cpf.ReadLine

        'create new external variable file
        NewExtVarFile = New IO.FileStream(DirPath & EVFilePrefix & FileNameTxt & EVFileSuffix & ".csv", IO.FileMode.Create)
        nev = New IO.StreamWriter(NewExtVarFile, System.Text.Encoding.Default)
        'write header row
        Select Case ModelArea
            Case "RoadLink"
                row = "FlowID,Yeary,PopZ1y,PopZ2y,GVAZ1y,GVAZ2y,Costy,MLanesy,DLanesy,SLanesy"
                nev.WriteLine(row)
            Case "RoadZone"
                row = "ZoneID,Yeary,PopZy,GVAZy,Costy,LaneKm,LKmMway,LKmRurA,LKmRurMin,LKmUrb,PCar,DCar,ECar,PLGV,DLGV,ELGV,DHGV,EHGV,DPSV,EPSV"
                nev.WriteLine(row)
            Case "RailLink"
                row = "FlowID,Yeary,Tracksy,PopZ1y,PopZ2y,GVAZ1y,GVAZ2y,Costy,CarFuely,MaxTDy"
                nev.WriteLine(row)
            Case "RailZone"
                row = "ZoneID,Yeary,PopZy,GvaZy,Costy,Stationsy,CarFuely"
                nev.WriteLine(row)
            Case "Airport"
                row = "Yeary,AirportID,GORPopy,GORGvay,Costy,TermCapy,MaxATMy,PlaneSizeDomy,PlaneSizeInty,LFDomy,LFInty"
                nev.WriteLine(row)
            Case "Seaport"
                row = "Yeary,PortID,LBCapy,DBCapy,GCCapy,LLCapy,RRCapy,GORPopy,GORGvay,Costy"
                nev.WriteLine(row)
        End Select
    End Sub

    Sub SetBaseCapacity()
        Select Case ModelArea
            Case "RoadLink"
                OldMLanes = EVDetails(7)
                OldDLanes = EVDetails(8)
                OldSLanes = EVDetails(9)
            Case "RoadZone"
                OldZMLKm = EVDetails(6)
                OldZRADLKm = EVDetails(7)
                OldZRASLKm = EVDetails(8)
                OldZRMLKm = EVDetails(9)
                OldZUDLKm = EVDetails(10)
                OldZUSLKm = EVDetails(11)
            Case "RailLink"
                OldTracks = EVDetails(2)
                OldMaxTD = EVDetails(9)
            Case "RailZone"
                OldStations = EVDetails(5)
            Case "Airport"
                BaseTermCap = EVDetails(5)
                BaseATM = EVDetails(6)
            Case "Seaport"
                OldLBCap = EVDetails(2)
                OldDBCap = EVDetails(3)
                OldGCCap = EVDetails(4)
                OldLLCap = EVDetails(5)
                OldRRCap = EVDetails(6)
        End Select
    End Sub

    Sub UpdateEVLine()
        Dim OldCap As Double
        Dim NewCap As Double

        Select Case ModelArea
            Case "RoadLink"
                OldCap = EVDetails(7)
                NewCap = CapChangeDetails(2)
                If FlowUpdated = True Then
                    NewMLanes = OldMLanes + NewCap
                Else
                    NewMLanes = OldCap + NewCap
                End If
                OldCap = EVDetails(8)
                NewCap = CapChangeDetails(3)
                If FlowUpdated = True Then
                    NewDLanes = OldDLanes + NewCap
                Else
                    NewDLanes = OldCap + NewCap
                End If
                OldCap = EVDetails(9)
                NewCap = CapChangeDetails(4)
                If FlowUpdated = True Then
                    NewSLanes = OldSLanes + NewCap
                Else
                    NewSLanes = OldCap + NewCap
                End If
            Case "RoadZone"
                OldCap = EVDetails(6)
                NewCap = CapChangeDetails(2)
                If FlowUpdated = True Then
                    NewZMLKm = OldZMLKm + NewCap
                Else
                    NewZMLKm = OldCap + NewCap
                End If
                OldCap = EVDetails(7)
                NewCap = CapChangeDetails(3)
                If FlowUpdated = True Then
                    NewZRADLKm = OldZRADLKm + NewCap
                Else
                    NewZRADLKm = OldCap + NewCap
                End If
                OldCap = EVDetails(8)
                NewCap = CapChangeDetails(4)
                If FlowUpdated = True Then
                    NewZRASLKm = OldZRASLKm + NewCap
                Else
                    NewZRASLKm = OldCap + NewCap
                End If
                OldCap = EVDetails(9)
                NewCap = CapChangeDetails(5)
                If FlowUpdated = True Then
                    NewZRMLKm = OldZRMLKm + NewCap
                Else
                    NewZRMLKm = OldCap + NewCap
                End If
                OldCap = EVDetails(10)
                NewCap = CapChangeDetails(6)
                If FlowUpdated = True Then
                    NewZUDLKm = OldZUDLKm + NewCap
                Else
                    NewZUDLKm = OldCap + NewCap
                End If
                OldCap = EVDetails(11)
                NewCap = CapChangeDetails(7)
                If FlowUpdated = True Then
                    NewZUSLKm = OldZUSLKm + NewCap
                Else
                    NewZUSLKm = OldCap + NewCap
                End If
                NewZoneLKm = NewZMLKm + NewZRADLKm + NewZRASLKm + NewZRMLKm + NewZUDLKm + NewZUSLKm
            Case "RailLink"
                'update number of tracks
                OldCap = EVDetails(2)
                NewCap = CapChangeDetails(2)
                If FlowUpdated = True Then
                    NewTracks = OldTracks + NewCap
                Else
                    NewTracks = OldCap + NewCap
                End If
                'update maximum train density
                OldCap = EVDetails(9)
                NewCap = CapChangeDetails(3)
                If FlowUpdated = True Then
                    NewMaxTD = OldMaxTD + NewCap
                Else
                    NewMaxTD = OldCap + NewCap
                End If
            Case "RailZone"
                'update number of stations
                OldCap = EVDetails(5)
                NewCap = CapChangeDetails(2)
                If FlowUpdated = True Then
                    NewStations = OldStations + NewCap
                Else
                    NewStations = OldCap + NewCap
                End If
                'update new trips field (note that this is only relevant in the year when a station opens
                NewTrips = CapChangeDetails(3) / NewCap
            Case "Airport"

            Case "Seaport"
                'update liquid bulk capacity
                OldCap = EVDetails(2)
                NewCap = CapChangeDetails(2)
                If FlowUpdated = True Then
                    NewLBCap = OldLBCap + NewCap
                Else
                    NewLBCap = OldCap + NewCap
                End If
                'update dry bulk capacity
                OldCap = EVDetails(3)
                NewCap = CapChangeDetails(3)
                If FlowUpdated = True Then
                    NewDBCap = OldDBCap + NewCap
                Else
                    NewDBCap = OldCap + NewCap
                End If
                'update general cargo capacity
                OldCap = EVDetails(4)
                NewCap = CapChangeDetails(4)
                If FlowUpdated = True Then
                    NewGCCap = OldGCCap + NewCap
                Else
                    NewGCCap = OldCap + NewCap
                End If
                'update lolo capacity
                OldCap = EVDetails(5)
                NewCap = CapChangeDetails(5)
                If FlowUpdated = True Then
                    NewLLCap = OldLLCap + NewCap
                Else
                    NewLLCap = OldCap + NewCap
                End If
                'update roro capacity
                OldCap = EVDetails(6)
                NewCap = CapChangeDetails(6)
                If FlowUpdated = True Then
                    NewRRCap = OldRRCap + NewCap
                Else
                    NewRRCap = OldCap + NewCap
                End If
        End Select
    End Sub

    Sub WriteOutputLine()

        Dim NewEVLine As String
        Dim ArrayCount As Integer

        If FlowUpdated = False Then
            nev.WriteLine(EVLine)
        Else
            Select Case ModelArea
                Case "RoadLink"
                    NewEVLine = EVDetails(0)
                    For ArrayCount = 1 To 6
                        NewEVLine = NewEVLine & "," & EVDetails(ArrayCount)
                    Next
                    NewEVLine = NewEVLine & "," & NewMLanes & "," & NewDLanes & "," & NewSLanes
                    nev.WriteLine(NewEVLine)
                    OldMLanes = NewMLanes
                    OldDLanes = NewDLanes
                    OldSLanes = NewSLanes
                Case "RoadZone"
                    NewEVLine = EVDetails(0)
                    For ArrayCount = 1 To 4
                        NewEVLine = NewEVLine & "," & EVDetails(ArrayCount)
                    Next
                    NewEVLine = NewEVLine & "," & NewZoneLKm & "," & NewZMLKm & "," & NewZRADLKm & "," & NewZRASLKm & "," & NewZRMLKm & "," & NewZUDLKm & "," & NewZUSLKm
                    For ArrayCount = 12 To 21
                        NewEVLine = NewEVLine & "," & EVDetails(ArrayCount)
                    Next
                    nev.WriteLine(NewEVLine)
                    OldZMLKm = NewZMLKm
                    OldZRADLKm = NewZRADLKm
                    OldZRASLKm = NewZRASLKm
                    OldZRMLKm = NewZRMLKm
                    OldZUDLKm = NewZUDLKm
                    OldZUSLKm = NewZUSLKm
                Case "RailLink"
                    NewEVLine = EVDetails(0)
                    NewEVLine = NewEVLine & "," & EVDetails(1) & "," & NewTracks
                    For ArrayCount = 3 To 8
                        NewEVLine = NewEVLine & "," & EVDetails(ArrayCount)
                    Next
                    NewEVLine = NewEVLine & "," & NewMaxTD
                    nev.WriteLine(NewEVLine)
                    OldTracks = NewTracks
                    OldMaxTD = NewMaxTD
                Case "RailZone"
                    NewEVLine = EVDetails(0)
                    For ArrayCount = 1 To 4
                        NewEVLine = NewEVLine & "," & EVDetails(ArrayCount)
                    Next
                    NewEVLine = NewEVLine & "," & NewStations
                    NewEVLine = NewEVLine & "," & EVDetails(6)
                    NewEVLine = NewEVLine & "," & NewTrips
                    nev.WriteLine(NewEVLine)
                    OldStations = NewStations
                    'need to set newtrips to zero because it only applies in the year when a new station opens
                    NewTrips = 0
                Case "Airport"
                    NewEVLine = EVDetails(0)
                    For ArrayCount = 1 To 4
                        NewEVLine = NewEVLine & "," & EVDetails(ArrayCount)
                    Next
                    NewEVLine = NewEVLine & "," & (BaseTermCap + TermChange(EVDetails(1)))
                    NewEVLine = NewEVLine & "," & (BaseATM + ATMChange(EVDetails(1)))
                    For ArrayCount = 7 To 10
                        NewEVLine = NewEVLine & "," & EVDetails(ArrayCount)
                    Next
                    nev.WriteLine(NewEVLine)
                Case "Seaport"
                    NewEVLine = EVDetails(0)
                    NewEVLine = NewEVLine & "," & EVDetails(1)
                    NewEVLine = NewEVLine & "," & NewLBCap
                    NewEVLine = NewEVLine & "," & NewDBCap
                    NewEVLine = NewEVLine & "," & NewGCCap
                    NewEVLine = NewEVLine & "," & NewLLCap
                    NewEVLine = NewEVLine & "," & NewRRCap
                    For ArrayCount = 7 To 9
                        NewEVLine = NewEVLine & "," & EVDetails(ArrayCount)
                    Next
                    nev.WriteLine(NewEVLine)
                    OldLBCap = NewLBCap
                    OldDBCap = NewDBCap
                    OldGCCap = NewGCCap
                    OldLLCap = NewLLCap
                    OldRRCap = NewRRCap
            End Select
        End If

    End Sub

End Module
