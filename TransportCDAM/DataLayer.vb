Imports System.IO

Module DataLayer


    '****************************************************************************************
    ' Function: ReadData 
    '
    ' Purpose: Get an array of data from a csv file - to be replaced with database calls
    ' 
    ' Parameters:   Type - type of data (e.g. Road, Rail)
    '               SubType - subtype of data
    '               DataArray - array of data to be output
    '               HasHeaders - TRUE - input file has headers, FALSE - file has no headers
    '               datatype - variant type of data in array (e.g. string, integer)
    '               Connection - file path - to be replaced with database connection string
    '****************************************************************************************

    Function ReadData(ByVal Type As String, ByVal SubType As String, ByRef DataArray(,) As String,
                       Optional ByVal HasHeaders As Boolean = True,
                       Optional ByVal datatype As VariantType = VariantType.String,
                       Optional Connection As String = "") As Boolean
        Dim TheFileName As String = ""
        Dim DataFile As FileStream
        Dim DataRead As StreamReader
        Dim dbheadings As String
        Dim dbline As String
        Dim dbarray() As String
        Dim iR As Integer = 0, iC As Integer = 0
        Dim DataRows As Integer = 0, DataColumns As Integer = 0

        'Check if file path has been selected - if not then use default.
        If Connection = "" Then
            Connection = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Inputs\"
        End If
        'Make sure the file path ends with at \
        If Connection.Substring(Len(Connection) - 1, 1) <> "\" Then
            Connection = Connection & "\"
        End If

        'Get the filename of datafile based on Type and SubType
        'TODO - replace with database calls
        Select Case Type
            Case "Demographics"
                Select Case SubType
                    Case "Zone"
                        TheFileName = "ZoneScenarioPopFile.csv"
                    Case Else
                End Select
            Case "Economics"
                Select Case SubType
                    Case "Zone"
                        TheFileName = "ZoneScenarioEcoFile.csv"
                    Case Else
                End Select
            Case "Road"
                Select Case SubType
                    Case "Initial"
                        TheFileName = "RoadInputDataInitial.csv"
                    Case Else
                End Select
            Case Else

        End Select

        'Get file data
        Try
            DataFile = New FileStream(Connection & TheFileName, FileMode.Open, FileAccess.Read)
        Catch exIO As IOException
            MsgBox("An error was encountered trying to access the file " & Connection & TheFileName)
        End Try
        DataRead = New StreamReader(DataFile, System.Text.Encoding.Default)
        'Get line count
        DataRows = DataFile.Length

        'read header row
        dbheadings = DataRead.ReadLine
        dbarray = Split(dbheadings, ",")
        DataColumns = dbarray.Length

        'loop through row to det data

        For iR = 0 To DataRows - 1
            If DataArray Is Nothing Then
                ReDim DataArray(DataColumns - 1, 0)
            Else
                'iActualR is the actual number or data rows (not equal to iR if there are Error rows)
                ReDim Preserve DataArray(DataColumns - 1, iR)
            End If

            'Get a line of data from file
            dbline = DataRead.ReadLine
            If dbline Is Nothing Then
                Exit For
            End If
            dbarray = Split(dbline, ",")
            For iC = 0 To DataColumns - 1
                DataArray(iC, iR) = CStr(UnNull(dbarray(iC).ToString, VariantType.Char))
            Next
        Next

        DataRead.Close()

        Return True

    End Function

    '****************************************************************************************
    ' Function: WriteData 
    '
    ' Purpose: Output an array to a csv file - to be replaced with database calls
    ' 
    ' Parameters:   Type - type of data (e.g. Road, Rail)
    '               SubType - subtype of data
    '               DataArray - array of data to be output
    '               IsNewFile - TRUE - create a new file, FALSE - update and existing file
    '               FilePrefix - Optional fileprefix for output file (use date and time otherwise)
    '               Connection - file path - to be replaced with database connection string
    '****************************************************************************************

    Function WriteData(ByVal Type As String, ByVal SubType As String, ByRef DataArray(,) As String, ByVal IsNewFile As Boolean,
                       Optional ByVal FilePrefix As String = "", Optional Connection As String = "") As Boolean

        Dim OutFileName As String = "", TempFileName As String = ""
        Dim TempFile As FileStream, DataFile As FileStream
        Dim Template As StreamReader
        Dim DataWrite As StreamWriter
        Dim headings As String, Line As String = ""
        Dim headarray As String()
        Dim headcount As Integer, fieldcount As Integer
        Dim ix As Integer, iy As Integer

        'Check if file path has been selected - if not then use default.
        If Connection = "" Then
            Connection = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Outputs\"
        End If
        'Make sure the file path ends with at \
        If Connection.Substring(Len(Connection) - 1, 1) <> "\" Then
            Connection = Connection & "\"
        End If

        'Get the filename of datafile based on Type and SubType
        'TODO - replace with database calls
        Select Case Type
            Case "Road"
                Select Case SubType
                    Case "Output"
                        OutFileName = "RoadOutputData.csv"
                        TempFileName = "RoadOutputTemplate.csv"
                    Case Else
                End Select
            Case Else

        End Select

        'Check if prefix has been set - if not then use default
        If FilePrefix = "" Then
            FilePrefix = System.DateTime.Now.Year & System.DateTime.Now.Month & System.DateTime.Now.Day & System.DateTime.Now.Hour & System.DateTime.Now.Minute & System.DateTime.Now.Second
        End If
        'Add File prefix to Output Filename
        OutFileName = FilePrefix & OutFileName
        DataFile = File.Create(OutFileName)
        'DataFile = New FileStream(Connection & OutFileName, FileMode.Open, FileAccess.Write)
        DataWrite = New StreamWriter(DataFile, System.Text.Encoding.Default)
        'Get field count from Array
        fieldcount = UBound(DataArray, 1) + 1

        'TODO - Not needed for SoS version using database
        'If creating a new file then create headers
        If IsNewFile Then
            'Get the file headers from the template version of the output file
            TempFile = New FileStream(Connection & TempFileName, FileMode.Open, FileAccess.Read)
            Template = New StreamReader(TempFile, System.Text.Encoding.Default)
            'read header row from template
            headings = Template.ReadLine
            headarray = Split(headings, ",")
            headcount = headarray.Count

            'Write header line to output file
            DataWrite.WriteLine(headings)

            'check to make sure field count is the same as the header count
            If fieldcount <> headcount Then
                MsgBox("Template fields do not match output data fields")
                Return False
            End If
        End If


        'loop through array to generate lines in output file
        For iy = 0 To UBound(DataArray, 2)
            'Build a line to write
            Line = ""
            For ix = 0 To UBound(DataArray, 1)
                Line += UnNull(DataArray(ix, iy), VariantType.String) & ","
            Next
            'Delete the last comma
            Line = Line.Substring(0, Len(Line) - 1)
            'Write the line to the output file
            DataWrite.Write(Line)
            DataWrite.Write(vbCrLf)
        Next

        DataWrite.Close()

        Return True

    End Function

    Public Function UnNull(ByVal vntData As Object, ByVal datatype As VariantType) As Object
        'default
        UnNull = vntData

        If IsDBNull(vntData) Or IsNothing(vntData) Then
            Select Case datatype
                Case vbString
                    UnNull = ""
                Case vbDate
                    UnNull = "1/1/1900"
                Case vbSingle, vbInteger, vbLong, vbByte, vbCurrency, vbDecimal, vbDouble
                    UnNull = 0
                Case vbBoolean
                    UnNull = False
                Case Else
                    UnNull = ""
            End Select

        End If
    End Function


End Module
