Module TransportCDAM

    Sub GetParameters()
        Dim ary As String(,) = Nothing
        Dim i As Integer
        Dim ParamName As String

        'get plan details from the database
        ReadData("Inputs", "Parameters", ary)

        For i = 1 To UBound(ary, 2)
            ParamName = CStr(ary(3, i))
            Select Case ParamName
                Case "RunRoadLink"
                    RunRoadLink = CBool(ary(5, i))
                Case "RunRoadZone"
                    RunRoadZone = CBool(ary(5, i))
                Case "RunRailLink"
                    RunRailLink = CBool(ary(5, i))
                Case "RunRailZone"
                    RunRailZone = CBool(ary(5, i))
                Case "RunAir"
                    RunAir = CBool(ary(5, i))
                Case "RunSea"
                    RunSea = CBool(ary(5, i))
                Case "BuildInfra"
                    BuildInfra = CBool(ary(5, i))
                Case "CUCritValue"
                    CUCritValue = CDbl(ary(5, i))
                Case "VariableEl"
                    VariableEl = CBool(ary(5, i))
                Case "ElCritValue"
                    ElCritValue = CDbl(ary(5, i))
                Case "CongestionCharge"
                    CongestionCharge = CBool(ary(5, i))
                Case "ConChargeYear"
                    ConChargeYear = CInt(ary(5, i))
                Case Else
                    '....
            End Select
        Next

    End Sub

End Module
