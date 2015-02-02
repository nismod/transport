Public Class MainForm

    Private Sub Button1_Click(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles Button1.Click
        RunModel = True
        'Check if file path has been selected - if not then use default.
        If DirPath = "" Then
            DirPath = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Inputs\"
        End If
        'Check if prefix has been set - if not then use default
        If FilePrefix = "" Then
            FilePrefix = System.DateTime.Now.Millisecond
        End If

        'Just use dummy values for ModelRunID and Year for now
        Call runCDAM(1, StartYear)

        End
    End Sub

    Private Sub Button2_Click(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles Button2.Click
        End
    End Sub

    Private Sub CheckBox1_CheckedChanged(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles CheckBox1.CheckedChanged
        If CheckBox1.Checked = True Then
            RunRoadLink = True
        Else
            RunRoadLink = False
        End If
    End Sub

    Private Sub CheckBox2_CheckedChanged(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles CheckBox2.CheckedChanged
        If CheckBox2.Checked = True Then
            RunRoadZone = True
        Else
            RunRoadZone = False
        End If
    End Sub

    Private Sub CheckBox3_CheckedChanged(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles CheckBox3.CheckedChanged
        If CheckBox3.Checked = True Then
            RunRailLink = True
        Else
            RunRailLink = False
        End If
    End Sub

    Private Sub CheckBox4_CheckedChanged(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles CheckBox4.CheckedChanged
        If CheckBox4.Checked = True Then
            RunRailZone = True
        Else
            RunRailZone = False
        End If
    End Sub

    Private Sub CheckBox5_CheckedChanged(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles CheckBox5.CheckedChanged
        If CheckBox5.Checked = True Then
            RunAir = True
        Else
            RunAir = False
        End If
    End Sub

    Private Sub CheckBox6_CheckedChanged(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles CheckBox6.CheckedChanged
        If CheckBox6.Checked = True Then
            RunSea = True
        Else
            RunSea = False
        End If
    End Sub

    Private Sub folderPath_Click(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles folderPath.Click
        'FolderBrowserDialog1.SelectedPath = "C:\Users\cenv0384\Documents\Visual Studio 2013\Projects\ITRCWS1C3\Transport\TransportCDAM\Model Input and Output\Model Inputs"
        FolderBrowserDialog1.ShowDialog()
        DirPath = FolderBrowserDialog1.SelectedPath & "\"
    End Sub

    Private Sub TextBox1_TextChanged(ByVal sender As System.Object, ByVal e As System.EventArgs) Handles TextBox1.TextChanged
        FilePrefix = TextBox1.Text
    End Sub

    Private Sub ListBox1_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ListBox1.SelectedIndexChanged
        If ListBox1.SelectedIndex = 0 Then
            CreateExtVars = True
            UpdateExtVars = False
            'grey out main run button
            Button1.Enabled = False
            'grey out existing ev prefix box
            TextBox2.Enabled = False
            TextBox2.Text = ""
            Label1.Enabled = False
            'enable create ev box on second tab
            GroupBox5.Enabled = True
            'grey out update ev box on second tab
            GroupBox8.Enabled = False
            'enable model run buttons
            Button3.Enabled = True
            Button4.Enabled = True
            Button5.Enabled = False
            Button6.Enabled = False
            'enable SubStrategy combobox
            ComboBox27.Enabled = True
        ElseIf ListBox1.SelectedIndex = 1 Then
            CreateExtVars = False
            UpdateExtVars = True
            'grey out main run button
            Button1.Enabled = False
            'grey out existing ev prefix box
            TextBox2.Enabled = False
            TextBox2.Text = ""
            Label1.Enabled = False
            'grey out create ev box on second tab
            GroupBox5.Enabled = False
            'enable update ev box on second tab
            GroupBox8.Enabled = True
            'enable model run buttons
            Button3.Enabled = False
            Button4.Enabled = False
            Button5.Enabled = True
            Button6.Enabled = True
            'disable SubStrategy combobox
            ComboBox27.Enabled = True
        Else
            CreateExtVars = False
            UpdateExtVars = False
            'enable main run button
            Button1.Enabled = True
            'enable existing ev prefix box
            TextBox2.Enabled = True
            Label1.Enabled = True
            'grey out create ev box on second tab
            GroupBox5.Enabled = False
            'grey out update ev box on second tab
            GroupBox8.Enabled = False
            'enable model run buttons
            Button3.Enabled = False
            Button4.Enabled = False
            Button5.Enabled = False
            Button6.Enabled = False
            'disable SubStrategy combobox
            ComboBox27.Enabled = True
        End If
    End Sub

    Private Sub CheckBox7_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox7.CheckedChanged
        If CheckBox7.Checked = True Then
            NewRdLEV = True
            CheckBox13.Enabled = True
        Else
            NewRdLEV = False
            CheckBox13.Enabled = False
        End If
    End Sub

    Private Sub CheckBox8_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox8.CheckedChanged
        If CheckBox8.Checked = True Then
            NewRdZEV = True
            CheckBox14.Enabled = True
        Else
            NewRdZEV = False
            CheckBox14.Enabled = False
        End If
    End Sub

    Private Sub CheckBox9_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox9.CheckedChanged
        If CheckBox9.Checked = True Then
            NewRlLEV = True
            CheckBox15.Enabled = True
        Else
            NewRlLEV = False
            CheckBox15.Enabled = False
        End If
    End Sub

    Private Sub CheckBox10_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox10.CheckedChanged
        If CheckBox10.Checked = True Then
            NewRlZEV = True
            CheckBox16.Enabled = True
        Else
            NewRlZEV = False
            CheckBox16.Enabled = False
        End If
    End Sub

    Private Sub CheckBox11_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox11.CheckedChanged
        If CheckBox11.Checked = True Then
            NewAirEV = True
            CheckBox17.Enabled = True
        Else
            NewAirEV = False
            CheckBox17.Enabled = False
        End If
    End Sub

    Private Sub CheckBox12_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox12.CheckedChanged
        If CheckBox12.Checked = True Then
            NewSeaEV = True
            CheckBox18.Enabled = True
        Else
            NewSeaEV = False
            CheckBox18.Enabled = False
        End If
    End Sub

    Private Sub TextBox3_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox3.TextChanged
        EVFilePrefix = TextBox3.Text
    End Sub

    Private Sub TextBox2_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox2.TextChanged
        EVFilePrefix = TextBox2.Text
    End Sub

    Private Sub Button3_Click(sender As System.Object, e As System.EventArgs) Handles Button3.Click
        RunModel = False
        'Check if file path has been selected - if not then use default
        If DirPath = "" Then
            DirPath = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Inputs\"
        End If
        'Check if prefix has been set - if not then use default
        If FilePrefix = "" Then
            FilePrefix = System.DateTime.Now
        End If

        Call runCDAM(1, 2010)
        End
    End Sub

    Private Sub Button4_Click(sender As System.Object, e As System.EventArgs) Handles Button4.Click
        RunModel = True
        'Check if file path has been selected - if not then use default
        If DirPath = "" Then
            DirPath = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Inputs\"
        End If
        'Check if prefix has been set - if not then use default
        If FilePrefix = "" Then
            FilePrefix = System.DateTime.Now
        End If

        Call runCDAM(1, 2010)
        End
    End Sub

    Private Sub CheckBox13_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox13.CheckedChanged
        If CheckBox13.Checked = True Then
            NewRdLCap = True
            TextBox34.Enabled = True
            Label39.Enabled = True
        Else
            NewRdLCap = False
            TextBox34.Enabled = False
            Label39.Enabled = False
        End If
    End Sub

    Private Sub CheckBox14_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox14.CheckedChanged
        If CheckBox14.Checked = True Then
            NewRdZCap = True
        Else
            NewRdZCap = False
        End If
    End Sub

    Private Sub CheckBox15_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox15.CheckedChanged
        If CheckBox15.Checked = True Then
            NewRlLCap = True
            TextBox35.Enabled = True
            Label40.Enabled = True
        Else
            NewRlLCap = False
            TextBox35.Enabled = False
            Label40.Enabled = False
        End If
    End Sub

    Private Sub CheckBox16_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox16.CheckedChanged
        If CheckBox16.Checked = True Then
            NewRlZCap = True
        Else
            NewRlZCap = False
        End If
    End Sub

    Private Sub CheckBox17_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox17.CheckedChanged
        If CheckBox17.Checked = True Then
            NewAirCap = True
            TextBox36.Enabled = True
            TextBox37.Enabled = True
            Label41.Enabled = True
            Label42.Enabled = True
        Else
            NewAirCap = False
            TextBox36.Enabled = False
            TextBox37.Enabled = False
            Label41.Enabled = False
            Label42.Enabled = False
        End If
    End Sub

    Private Sub CheckBox18_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox18.CheckedChanged
        If CheckBox18.Checked = True Then
            NewSeaCap = True
            TextBox33.Enabled = True
            Label38.Enabled = True
        Else
            NewSeaCap = False
            TextBox33.Enabled = False
            Label38.Enabled = False
        End If
    End Sub

    Private Sub TextBox4_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox4.TextChanged
        CapFilePrefix = TextBox4.Text
    End Sub

    Private Sub CheckBox19_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox19.CheckedChanged
        If CheckBox19.Checked = True Then
            NewRdLCap = True
        Else
            NewRdLCap = False
        End If
    End Sub

    Private Sub CheckBox20_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox20.CheckedChanged
        If CheckBox20.Checked = True Then
            NewRdZCap = True
        Else
            NewRdZCap = False
        End If
    End Sub

    Private Sub CheckBox21_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox21.CheckedChanged
        If CheckBox21.Checked = True Then
            NewRlLCap = True
        Else
            NewRlLCap = False
        End If
    End Sub

    Private Sub CheckBox22_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox22.CheckedChanged
        If CheckBox22.Checked = True Then
            NewRlZCap = True
        Else
            NewRlZCap = False
        End If
    End Sub

    Private Sub CheckBox23_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox23.CheckedChanged
        If CheckBox23.Checked = True Then
            NewAirCap = True
        Else
            NewAirCap = False
        End If
    End Sub

    Private Sub CheckBox24_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox24.CheckedChanged
        If CheckBox24.Checked = True Then
            NewSeaCap = True
        Else
            NewSeaCap = False
        End If
    End Sub

    Private Sub Button5_Click(sender As System.Object, e As System.EventArgs) Handles Button5.Click
        RunModel = False
        'Check if file path has been selected - if not then use default
        If DirPath = "" Then
            DirPath = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Inputs\"
        End If
        'Check if prefix has been set - if not then use default
        If FilePrefix = "" Then
            FilePrefix = System.DateTime.Now
        End If

        Call runCDAM(1, 2010)
        End
    End Sub

    Private Sub Button6_Click(sender As System.Object, e As System.EventArgs) Handles Button6.Click
        RunModel = True
        'Check if file path has been selected - if not then use default
        If DirPath = "" Then
            DirPath = "\\soton.ac.uk\ude\PersonalFiles\Users\spb1g09\mydocuments\Southampton Work\ITRC\Transport CDAM\Model Inputs\"
        End If
        'Check if prefix has been set - if not then use default
        If FilePrefix = "" Then
            FilePrefix = System.DateTime.Now
        End If
        Call runCDAM(1, 2010)
        End
    End Sub

    Private Sub ComboBox1_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox1.SelectedIndexChanged
        If ComboBox1.SelectedItem = "Scaling constant" Then
            RlZEVSource = "Constant"
            RlZPopSource = "Constant"
            RlZEcoSource = "Constant"
            RlZEneSource = "Constant"
        ElseIf ComboBox1.SelectedItem = "Input file" Then
            RlZEVSource = "File"
            RlZPopSource = "File"
            RlZEcoSource = "File"
            RlZEneSource = "File"
            RlZOthSource = "File"
            ComboBox26.SelectedItem = "File"
        ElseIf ComboBox1.SelectedItem = "Database input" Then
            RlZEVSource = "Database"
            RlZPopSource = "Database"
            RlZEcoSource = "Database"
            RlZEneSource = "Database"
        End If
    End Sub

    Private Sub ComboBox2_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox2.SelectedIndexChanged
        If ComboBox2.SelectedItem = "Scaling constant" Then
            RlLEVSource = "Constant"
            RlLPopSource = "Constant"
            RlLEcoSource = "Constant"
            RlLEneSource = "Constant"
        ElseIf ComboBox2.SelectedItem = "Input file" Then
            RlLEVSource = "File"
            RlLPopSource = "File"
            RlLEcoSource = "File"
            RlLEneSource = "File"
            RlLOthSource = "File"
            ComboBox25.SelectedItem = "File"
        ElseIf ComboBox2.SelectedItem = "Database input" Then
            RlLEVSource = "Database"
            RlLPopSource = "Database"
            RlLEcoSource = "Database"
            RlLEneSource = "Database"
        End If
    End Sub

    Private Sub ComboBox3_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox3.SelectedIndexChanged
        If ComboBox3.SelectedItem = "Scaling constant" Then
            RdLPopSource = "Constant"
            RdLEneSource = "Constant"
            RdLEcoSource = "Constant"
        ElseIf ComboBox3.SelectedItem = "Input file" Then
            RdLPopSource = "File"
            RdLEneSource = "File"
            RdLEcoSource = "File"
        ElseIf ComboBox3.SelectedItem = "Database input" Then
            RdLPopSource = "Database"
            RdLEneSource = "Database"
            RdLEcoSource = "Database"
        End If
    End Sub

    Private Sub ComboBox4_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox4.SelectedIndexChanged
        If ComboBox4.SelectedItem = "Scaling constant" Then
            RdZEVSource = "Constant"
            RdZPopSource = "Constant"
            RdZEneSource = "Constant"
            RdZEcoSource = "Constant"
        ElseIf ComboBox4.SelectedItem = "Input file" Then
            RdZEVSource = "File"
            RdZPopSource = "File"
            RdZEneSource = "File"
            RdZEcoSource = "File"
        ElseIf ComboBox4.SelectedItem = "Database input" Then
            RdZEVSource = "Database"
            RdZPopSource = "Database"
            RdZEneSource = "Database"
            RdZEcoSource = "Database"
        End If
    End Sub

    Private Sub ComboBox5_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox5.SelectedIndexChanged
        If ComboBox5.SelectedItem = "Scaling constant" Then
            AirEVSource = "Constant"
            AirPopSource = "Constant"
            AirEcoSource = "Constant"
            AirEneSource = "Constant"
        ElseIf ComboBox5.SelectedItem = "Input file" Then
            AirEVSource = "File"
            AirPopSource = "File"
            AirEcoSource = "File"
            AirEneSource = "File"
        ElseIf ComboBox5.SelectedItem = "Database input" Then
            AirEVSource = "Database"
            AirPopSource = "Database"
            AirEcoSource = "Database"
            AirEneSource = "Database"
        End If
    End Sub

    Private Sub ComboBox6_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox6.SelectedIndexChanged
        If ComboBox6.SelectedItem = "Scaling constant" Then
            SeaEVSource = "Constant"
            SeaPopSource = "Constant"
            SeaEcoSource = "Constant"
            SeaEneSource = "Constant"
        ElseIf ComboBox6.SelectedItem = "Input file" Then
            SeaEVSource = "File"
            SeaPopSource = "File"
            SeaEcoSource = "File"
            SeaEneSource = "File"
        ElseIf ComboBox6.SelectedItem = "Database input" Then
            SeaEVSource = "Database"
            SeaPopSource = "Database"
            SeaEcoSource = "Database"
            SeaEneSource = "Database"
        End If
    End Sub

    Private Sub Button7_Click(sender As System.Object, e As System.EventArgs) Handles Button7.Click
        Dim openfdpop As New OpenFileDialog()

        openfdpop.InitialDirectory = "c:\"
        openfdpop.Filter = "csv files (*.csv)|*.csv|All files (*.*)|*.*"
        openfdpop.FilterIndex = 1
        openfdpop.RestoreDirectory = True

        If openfdpop.ShowDialog() = System.Windows.Forms.DialogResult.OK Then
            TextBox6.Text = openfdpop.FileName
        End If

    End Sub

    Private Sub CheckBox25_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox25.CheckedChanged
        If CheckBox25.Checked = True Then
            Button7.Enabled = True
            DBasePop = True
            ComboBox7.Enabled = False
            ComboBox10.Enabled = False
            ComboBox13.Enabled = False
            ComboBox16.Enabled = False
            ComboBox19.Enabled = False
            ComboBox22.Enabled = False
            CheckBox37.Enabled = True
        Else
            Button7.Enabled = False
            DBasePop = False
            ComboBox7.Enabled = True
            ComboBox10.Enabled = True
            ComboBox13.Enabled = True
            ComboBox16.Enabled = True
            ComboBox19.Enabled = True
            ComboBox22.Enabled = True
            CheckBox37.Enabled = False
        End If
    End Sub

    Private Sub Button8_Click(sender As System.Object, e As System.EventArgs) Handles Button8.Click
        Dim openfdeco As New OpenFileDialog()

        openfdeco.InitialDirectory = "c:\"
        openfdeco.Filter = "csv files (*.csv)|*.csv|All files (*.*)|*.*"
        openfdeco.FilterIndex = 1
        openfdeco.RestoreDirectory = True

        If openfdeco.ShowDialog() = System.Windows.Forms.DialogResult.OK Then
            TextBox7.Text = openfdeco.FileName
        End If

    End Sub

    Private Sub CheckBox26_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox26.CheckedChanged
        If CheckBox26.Checked = True Then
            Button8.Enabled = True
            DBaseEco = True
            ComboBox8.Enabled = False
            ComboBox11.Enabled = False
            ComboBox14.Enabled = False
            ComboBox17.Enabled = False
            ComboBox20.Enabled = False
            ComboBox23.Enabled = False
        Else
            Button8.Enabled = False
            DBaseEco = False
            ComboBox8.Enabled = True
            ComboBox11.Enabled = True
            ComboBox14.Enabled = True
            ComboBox17.Enabled = True
            ComboBox20.Enabled = True
            ComboBox23.Enabled = True
        End If
    End Sub

    Private Sub Button9_Click(sender As System.Object, e As System.EventArgs) Handles Button9.Click
        Dim openfdene As New OpenFileDialog()

        openfdene.InitialDirectory = "c:\"
        openfdene.Filter = "csv files (*.csv)|*.csv|All files (*.*)|*.*"
        openfdene.FilterIndex = 1
        openfdene.RestoreDirectory = True

        If openfdene.ShowDialog() = System.Windows.Forms.DialogResult.OK Then
            TextBox8.Text = openfdene.FileName
        End If

    End Sub

    Private Sub CheckBox27_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox27.CheckedChanged
        If CheckBox27.Checked = True Then
            Button9.Enabled = True
            DBaseEne = True
            ComboBox9.Enabled = False
            ComboBox12.Enabled = False
            ComboBox15.Enabled = False
            ComboBox18.Enabled = False
            ComboBox21.Enabled = False
            ComboBox24.Enabled = False
        Else
            Button9.Enabled = False
            DBaseEne = False
            ComboBox9.Enabled = True
            ComboBox12.Enabled = True
            ComboBox15.Enabled = True
            ComboBox18.Enabled = True
            ComboBox21.Enabled = True
            ComboBox24.Enabled = True
        End If
    End Sub

    Private Sub TextBox6_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox6.TextChanged
        DBasePopFile = TextBox6.Text
    End Sub

    Private Sub TextBox7_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox7.TextChanged
        DBaseEcoFile = TextBox7.Text
    End Sub

    Private Sub TextBox8_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox8.TextChanged
        DBaseEneFile = TextBox8.Text
    End Sub

    Private Sub CheckBox28_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox28.CheckedChanged
        If CheckBox28.Checked = True Then
            Button10.Enabled = True
            DBasePopG = True
            ComboBox7.Enabled = False
            ComboBox10.Enabled = False
            ComboBox13.Enabled = False
            ComboBox16.Enabled = False
            ComboBox19.Enabled = False
            ComboBox22.Enabled = False
        Else
            Button10.Enabled = False
            DBasePopG = False
            ComboBox7.Enabled = True
            ComboBox10.Enabled = True
            ComboBox13.Enabled = True
            ComboBox16.Enabled = True
            ComboBox19.Enabled = True
            ComboBox22.Enabled = True
        End If
    End Sub

    Private Sub TextBox9_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox9.TextChanged
        DBasePopGFile = TextBox9.Text
    End Sub

    Private Sub Button10_Click(sender As System.Object, e As System.EventArgs) Handles Button10.Click
        Dim openfdpopg As New OpenFileDialog()

        openfdpopg.InitialDirectory = "c:\"
        openfdpopg.Filter = "csv files (*.csv)|*.csv|All files (*.*)|*.*"
        openfdpopg.FilterIndex = 1
        openfdpopg.RestoreDirectory = True

        If openfdpopg.ShowDialog() = System.Windows.Forms.DialogResult.OK Then
            TextBox9.Text = openfdpopg.FileName
        End If
    End Sub

    Private Sub ComboBox7_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox7.SelectedIndexChanged
        If ComboBox7.SelectedItem = "Constant" Then
            RdLPopSource = "Constant"
        ElseIf ComboBox7.SelectedItem = "File" Then
            RdLPopSource = "File"
        End If
    End Sub

    Private Sub ComboBox8_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox8.SelectedIndexChanged
        If ComboBox8.SelectedItem = "Constant" Then
            RdLEcoSource = "Constant"
        ElseIf ComboBox8.SelectedItem = "File" Then
            RdLEcoSource = "File"
        End If
    End Sub

    Private Sub ComboBox9_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox9.SelectedIndexChanged
        If ComboBox9.SelectedItem = "Constant" Then
            RdLEneSource = "Constant"
        ElseIf ComboBox9.SelectedItem = "File" Then
            RdLEneSource = "File"
        End If
    End Sub

    Private Sub ComboBox10_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox10.SelectedIndexChanged
        If ComboBox10.SelectedItem = "Constant" Then
            RdZPopSource = "Constant"
        ElseIf ComboBox10.SelectedItem = "File" Then
            RdZPopSource = "File"
        End If
    End Sub

    Private Sub ComboBox11_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox11.SelectedIndexChanged
        If ComboBox11.SelectedItem = "Constant" Then
            RdZEcoSource = "Constant"
        ElseIf ComboBox11.SelectedItem = "File" Then
            RdZEcoSource = "File"
        End If
    End Sub

    Private Sub ComboBox12_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox12.SelectedIndexChanged
        If ComboBox12.SelectedItem = "Constant" Then
            RdZEneSource = "Constant"
        ElseIf ComboBox12.SelectedItem = "File" Then
            RdZEneSource = "File"
        End If
    End Sub

    Private Sub ComboBox13_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox13.SelectedIndexChanged
        If ComboBox13.SelectedItem = "Constant" Then
            RlLPopSource = "Constant"
        ElseIf ComboBox13.SelectedItem = "File" Then
            RlLPopSource = "File"
        End If
    End Sub

    Private Sub ComboBox14_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox14.SelectedIndexChanged
        If ComboBox14.SelectedItem = "Constant" Then
            RlLEcoSource = "Constant"
        ElseIf ComboBox14.SelectedItem = "File" Then
            RlLEcoSource = "File"
        End If
    End Sub

    Private Sub ComboBox15_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox15.SelectedIndexChanged
        If ComboBox15.SelectedItem = "Constant" Then
            RlLEneSource = "Constant"
        ElseIf ComboBox15.SelectedItem = "File" Then
            RlLEneSource = "File"
        End If
    End Sub

    Private Sub ComboBox16_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox16.SelectedIndexChanged
        If ComboBox16.SelectedItem = "Constant" Then
            RlZPopSource = "Constant"
        ElseIf ComboBox16.SelectedItem = "File" Then
            RlZPopSource = "File"
        End If
    End Sub

    Private Sub ComboBox17_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox17.SelectedIndexChanged
        If ComboBox17.SelectedItem = "Constant" Then
            RlZEcoSource = "Constant"
        ElseIf ComboBox17.SelectedItem = "File" Then
            RlZEcoSource = "File"
        End If
    End Sub

    Private Sub ComboBox18_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox18.SelectedIndexChanged
        If ComboBox18.SelectedItem = "Constant" Then
            RlZEneSource = "Constant"
        ElseIf ComboBox18.SelectedItem = "File" Then
            RlZEneSource = "File"
        End If
    End Sub

    Private Sub ComboBox19_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox19.SelectedIndexChanged
        If ComboBox19.SelectedItem = "Constant" Then
            AirPopSource = "Constant"
        ElseIf ComboBox19.SelectedItem = "File" Then
            AirPopSource = "File"
        End If
    End Sub

    Private Sub ComboBox20_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox20.SelectedIndexChanged
        If ComboBox20.SelectedItem = "Constant" Then
            AirEcoSource = "Constant"
        ElseIf ComboBox20.SelectedItem = "File" Then
            AirEcoSource = "File"
        End If
    End Sub

    Private Sub ComboBox21_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox21.SelectedIndexChanged
        If ComboBox21.SelectedItem = "Constant" Then
            AirEneSource = "Constant"
        ElseIf ComboBox21.SelectedItem = "File" Then
            AirEneSource = "File"
        End If
    End Sub

    Private Sub ComboBox22_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox22.SelectedIndexChanged
        If ComboBox22.SelectedItem = "Constant" Then
            SeaPopSource = "Constant"
        ElseIf ComboBox22.SelectedItem = "File" Then
            SeaPopSource = "File"
        End If
    End Sub

    Private Sub ComboBox23_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox23.SelectedIndexChanged
        If ComboBox23.SelectedItem = "Constant" Then
            SeaEcoSource = "Constant"
        ElseIf ComboBox23.SelectedItem = "File" Then
            SeaEcoSource = "File"
        End If
    End Sub

    Private Sub ComboBox24_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox24.SelectedIndexChanged
        If ComboBox24.SelectedItem = "Constant" Then
            SeaEneSource = "Constant"
        ElseIf ComboBox24.SelectedItem = "File" Then
            SeaEneSource = "File"
        End If
    End Sub

    Private Sub ComboBox25_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox25.SelectedIndexChanged
        If ComboBox25.SelectedItem = "File" Then
            RlLOthSource = "File"
        ElseIf ComboBox25.SelectedItem = "None" Then
            RlLOthSource = ""
        End If
    End Sub

    Private Sub ComboBox26_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox26.SelectedIndexChanged
        If ComboBox26.SelectedItem = "File" Then
            RlZOthSource = "File"
        ElseIf ComboBox26.SelectedItem = "None" Then
            RlZOthSource = ""
        End If
    End Sub

    Private Sub OpenControlFileToolStripMenuItem_Click(sender As System.Object, e As System.EventArgs) Handles OpenControlFileToolStripMenuItem.Click
        Dim opencontf As New OpenFileDialog()

        opencontf.InitialDirectory = "C:\"
        'opencontf.InitialDirectory = "C:\Users\cenv0384\Documents\Visual Studio 2013\Projects\ITRCWS1C3\Transport\TransportCDAM\Model Input and Output"
        opencontf.Filter = "icf files (*.icf)|*.icf"
        opencontf.FilterIndex = 1
        opencontf.RestoreDirectory = True

        If opencontf.ShowDialog() = System.Windows.Forms.DialogResult.OK Then
            ControlFile = CType(opencontf.OpenFile(), System.IO.FileStream)
            Call OpenControlFile()
        End If
    End Sub

    Private Sub SaveControlFileToolStripMenuItem_Click(sender As System.Object, e As System.EventArgs) Handles SaveControlFileToolStripMenuItem.Click
        Dim savecontf As New SaveFileDialog()

        savecontf.InitialDirectory = "c:\"
        savecontf.Filter = "icf files (*.icf)|*.icf"
        savecontf.FilterIndex = 1
        savecontf.RestoreDirectory = True

        If savecontf.ShowDialog() = System.Windows.Forms.DialogResult.OK Then
            ControlFile = CType(savecontf.OpenFile(), System.IO.FileStream)
            Call SaveControlFile()
        End If
    End Sub

    Private Sub ExitToolStripMenuItem_Click(sender As System.Object, e As System.EventArgs) Handles ExitToolStripMenuItem.Click
        Dim savecontf As New SaveFileDialog()

        MsgBox("Do you want to save the model control file?", MsgBoxStyle.YesNo)
        If MsgBoxResult.Yes = True Then
            savecontf.InitialDirectory = "c:\"
            savecontf.Filter = "icf files (*.icf)|*.icf"
            savecontf.FilterIndex = 1
            savecontf.RestoreDirectory = True

            If savecontf.ShowDialog() = System.Windows.Forms.DialogResult.OK Then
                ControlFile = CType(savecontf.OpenFile(), System.IO.FileStream)
                Call SaveControlFile()
            End If
            End
        Else
            End
        End If
    End Sub

    Sub SaveControlFile()
        Dim cfs As IO.StreamWriter
        Dim controlstring As String

        cfs = New IO.StreamWriter(ControlFile, System.Text.Encoding.Default)
        If CheckBox1.Checked = True Then
            controlstring = "roadlinkmodule,True"
        Else
            controlstring = "roadlinkmodule,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox2.Checked = True Then
            controlstring = "roadzonemodule,True"
        Else
            controlstring = "roadzonemodule,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox3.Checked = True Then
            controlstring = "raillinkmodule,True"
        Else
            controlstring = "raillinkmodule,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox4.Checked = True Then
            controlstring = "railzonemodule,True"
        Else
            controlstring = "railzonemodule,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox5.Checked = True Then
            controlstring = "airmodule,True"
        Else
            controlstring = "airmodule,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox6.Checked = True Then
            controlstring = "seamodule,True"
        Else
            controlstring = "seamodule,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "modelfiles," & DirPath
        cfs.WriteLine(controlstring)
        controlstring = "outputprefix," & TextBox1.Text
        cfs.WriteLine(controlstring)
        If ListBox1.SelectedIndex = 0 Then
            controlstring = "extvarfilesource,0"
        ElseIf ListBox1.SelectedIndex = 1 Then
            controlstring = "extvarfilesource,1"
        ElseIf ListBox1.SelectedIndex = 2 Then
            controlstring = "extvarfilesource,2"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "evprefix," & TextBox2.Text
        cfs.WriteLine(controlstring)
        If ComboBox27.SelectedItem = "TR0: Decline and Decay" Then
            controlstring = "SubStrategy,0"
        ElseIf ComboBox27.SelectedItem = "TR1: Predict and Provide" Then
            controlstring = "SubStrategy,1"
        ElseIf ComboBox27.SelectedItem = "TR2: Cost and Constrain" Then
            controlstring = "SubStrategy,2"
        ElseIf ComboBox27.SelectedItem = "TR3: Adapting the Fleet" Then
            controlstring = "SubStrategy,3"
        ElseIf ComboBox27.SelectedItem = "TR4: Promo-Pricing" Then
            controlstring = "SubStrategy,4"
        ElseIf ComboBox27.SelectedItem = "TR5: Connected Grid" Then
            controlstring = "SubStrategy,5"
        ElseIf ComboBox27.SelectedItem = "TR6: Smarter Choices" Then
            controlstring = "SubStrategy,6"
        Else
            controlstring = "SubStrategy,"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox29.Checked = True Then
            controlstring = "autoinfra,True"
        Else
            controlstring = "autoinfra,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "infraper," & TextBox10.Text
        cfs.WriteLine(controlstring)
        If CheckBox30.Checked = True Then
            controlstring = "varel,True"
        Else
            controlstring = "varel,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "varelrat," & TextBox11.Text
        cfs.WriteLine(controlstring)
        If CheckBox31.Checked = True Then
            controlstring = "rdconcharge,True"
        Else
            controlstring = "rdconcharge,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "rdconcyear," & TextBox28.Text
        cfs.WriteLine(controlstring)
        controlstring = "rdconcper," & TextBox12.Text
        cfs.WriteLine(controlstring)
        If CheckBox32.Checked = True Then
            controlstring = "rdemcharge,True"
        Else
            controlstring = "rdemcharge,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "rdemchyr," & TextBox27.Text
        cfs.WriteLine(controlstring)
        If CheckBox33.Checked = True Then
            controlstring = "rdwppl,True"
        Else
            controlstring = "rdwppl,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "wpplyear," & TextBox13.Text
        cfs.WriteLine(controlstring)
        controlstring = "wpplper," & TextBox14.Text
        cfs.WriteLine(controlstring)
        If CheckBox34.Checked = True Then
            controlstring = "rlconcharge,True"
        Else
            controlstring = "rlconcharge,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "rlconcyear," & TextBox29.Text
        cfs.WriteLine(controlstring)
        controlstring = "rlconcper," & TextBox15.Text
        cfs.WriteLine(controlstring)
        If CheckBox41.Checked = True Then
            controlstring = "rlemcharge,True"
        Else
            controlstring = "rlemcharge,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "rlemchyr," & TextBox30.Text
        cfs.WriteLine(controlstring)
        If CheckBox35.Checked = True Then
            controlstring = "airconcharge,True"
        Else
            controlstring = "airconcharge,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "airconcyr," & TextBox31.Text
        cfs.WriteLine(controlstring)
        controlstring = "airconcper," & TextBox16.Text
        cfs.WriteLine(controlstring)
        If CheckBox42.Checked = True Then
            controlstring = "airemcharge,True"
        Else
            controlstring = "airemcharge,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "airemcyr," & TextBox32.Text
        cfs.WriteLine(controlstring)
        If CheckBox38.Checked = True Then
            controlstring = "smartchoice,True"
        Else
            controlstring = "smartchoice,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "smartchoiceintro," & TextBox18.Text
        cfs.WriteLine(controlstring)
        controlstring = "smartchoiceper," & TextBox19.Text
        cfs.WriteLine(controlstring)
        controlstring = "smartchoiceyears," & TextBox20.Text
        cfs.WriteLine(controlstring)
        If CheckBox39.Checked = True Then
            controlstring = "smartlogistics,True"
        Else
            controlstring = "smartlogistics,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "smartlogintro," & TextBox23.Text
        cfs.WriteLine(controlstring)
        controlstring = "smartlogper," & TextBox22.Text
        cfs.WriteLine(controlstring)
        controlstring = "smartlogyears," & TextBox21.Text
        cfs.WriteLine(controlstring)
        If CheckBox40.Checked = True Then
            controlstring = "urbfrtinn,True"
        Else
            controlstring = "urbfrtinn,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "urbfrtinnintro," & TextBox26.Text
        cfs.WriteLine(controlstring)
        controlstring = "urbfrtinnper," & TextBox25.Text
        cfs.WriteLine(controlstring)
        controlstring = "urbfrtinnyears," & TextBox24.Text
        cfs.WriteLine(controlstring)
        If CheckBox7.Checked = True Then
            controlstring = "newrdlfile,True"
        Else
            controlstring = "newrdlfile,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox8.Checked = True Then
            controlstring = "newrdzfile,True"
        Else
            controlstring = "newrdzfile,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox9.Checked = True Then
            controlstring = "newrllfile,True"
        Else
            controlstring = "newrllfile,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox10.Checked = True Then
            controlstring = "newrlzfile,True"
        Else
            controlstring = "newrlzfile,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox11.Checked = True Then
            controlstring = "newairfile,True"
        Else
            controlstring = "newairfile,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox12.Checked = True Then
            controlstring = "newseafile,True"
        Else
            controlstring = "newseafile,False"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox3.SelectedItem = "Scaling constant" Then
            controlstring = "rdlsource,constant"
        ElseIf ComboBox3.SelectedItem = "Input file" Then
            controlstring = "rdlsource,file"
        ElseIf ComboBox3.SelectedItem = "Database input" Then
            controlstring = "rdlsource,database"
        Else
            controlstring = "rdlsource,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox4.SelectedItem = "Scaling constant" Then
            controlstring = "rdzsource,constant"
        ElseIf ComboBox4.SelectedItem = "Input file" Then
            controlstring = "rdzsource,file"
        ElseIf ComboBox4.SelectedItem = "Database input" Then
            controlstring = "rdzsource,database"
        Else
            controlstring = "rdzsource,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox2.SelectedItem = "Scaling constant" Then
            controlstring = "rllsource,constant"
        ElseIf ComboBox2.SelectedItem = "Input file" Then
            controlstring = "rllsource,file"
        ElseIf ComboBox2.SelectedItem = "Database input" Then
            controlstring = "rllsource,database"
        Else
            controlstring = "rllsource,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox1.SelectedItem = "Scaling constant" Then
            controlstring = "rlzsource,constant"
        ElseIf ComboBox1.SelectedItem = "Input file" Then
            controlstring = "rlzsource,file"
        ElseIf ComboBox1.SelectedItem = "Database input" Then
            controlstring = "rlzsource,database"
        Else
            controlstring = "rlzsource,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox5.SelectedItem = "Scaling constant" Then
            controlstring = "airsource,constant"
        ElseIf ComboBox5.SelectedItem = "Input file" Then
            controlstring = "airsource,file"
        ElseIf ComboBox5.SelectedItem = "Database input" Then
            controlstring = "airsource,database"
        Else
            controlstring = "airsource,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox6.SelectedItem = "Scaling constant" Then
            controlstring = "seasource,constant"
        ElseIf ComboBox6.SelectedItem = "Input file" Then
            controlstring = "seasource,file"
        ElseIf ComboBox6.SelectedItem = "Database input" Then
            controlstring = "seasource,database"
        Else
            controlstring = "seasource,"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "newevprefix," & TextBox3.Text
        cfs.WriteLine(controlstring)
        If CheckBox13.Checked = True Then
            controlstring = "newrdlinfra,True"
        Else
            controlstring = "newrdlinfra,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "newrdlanes," & TextBox34.Text
        cfs.WriteLine(controlstring)
        If CheckBox14.Checked = True Then
            controlstring = "newrdzinfra,True"
        Else
            controlstring = "newrdzinfra,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox15.Checked = True Then
            controlstring = "newrllinfra,True"
        Else
            controlstring = "newrllinfra,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "rltrackperyr," & TextBox35.Text
        cfs.WriteLine(controlstring)
        If CheckBox16.Checked = True Then
            controlstring = "newrlzinfra,True"
        Else
            controlstring = "newrlzinfra,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox17.Checked = True Then
            controlstring = "newairinfra,True"
        Else
            controlstring = "newairinfra,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "airrunperyr," & TextBox36.Text
        cfs.WriteLine(controlstring)
        controlstring = "airtermperyr," & TextBox37.Text
        cfs.WriteLine(controlstring)
        If CheckBox18.Checked = True Then
            controlstring = "newseainfra,True"
        Else
            controlstring = "newseainfra,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "newseaton," & TextBox33.Text
        cfs.WriteLine(controlstring)
        controlstring = "addinfraprefix," & TextBox4.Text
        cfs.WriteLine(controlstring)
        If CheckBox36.Checked = True Then
            controlstring = "railelec,True"
        Else
            controlstring = "railelec,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "railelkm," & TextBox17.Text
        cfs.WriteLine(controlstring)
        If CheckBox19.Checked = True Then
            controlstring = "addrdlinfra,True"
        Else
            controlstring = "addrdlinfra,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox20.Checked = True Then
            controlstring = "addrdzinfra,True"
        Else
            controlstring = "addrdzinfra,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox21.Checked = True Then
            controlstring = "addrllinfra,True"
        Else
            controlstring = "addrllinfra,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox22.Checked = True Then
            controlstring = "addrlzinfra,True"
        Else
            controlstring = "addrlzinfra,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox23.Checked = True Then
            controlstring = "addairinfra,True"
        Else
            controlstring = "addairinfra,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox24.Checked = True Then
            controlstring = "addseainfra,True"
        Else
            controlstring = "addseainfra,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "newinfraprefix," & TextBox5.Text
        cfs.WriteLine(controlstring)
        If CheckBox25.Checked = True Then
            controlstring = "popdincheck,True"
        Else
            controlstring = "popdincheck,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox37.Checked = True Then
            controlstring = "scotwaldisagg,True"
        Else
            controlstring = "scotwaldisagg,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox28.Checked = True Then
            controlstring = "popgincheck,True"
        Else
            controlstring = "popgincheck,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox26.Checked = True Then
            controlstring = "ecoincheck,True"
        Else
            controlstring = "ecoincheck,False"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox27.Checked = True Then
            controlstring = "eneincheck,True"
        Else
            controlstring = "eneincheck,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "popdfile," & TextBox6.Text
        cfs.WriteLine(controlstring)
        controlstring = "popgfile," & TextBox9.Text
        cfs.WriteLine(controlstring)
        controlstring = "ecofile," & TextBox7.Text
        cfs.WriteLine(controlstring)
        controlstring = "enefile," & TextBox8.Text
        cfs.WriteLine(controlstring)
        If ComboBox7.SelectedItem = "Constant" Then
            controlstring = "rdlpoi,constant"
        ElseIf ComboBox7.SelectedItem = "File" Then
            controlstring = "rdlpoi,file"
        Else
            controlstring = "rdlpoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox8.SelectedItem = "Constant" Then
            controlstring = "rdlecoi,constant"
        ElseIf ComboBox8.SelectedItem = "File" Then
            controlstring = "rdlecoi,file"
        Else
            controlstring = "rdlecoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox9.SelectedItem = "Constant" Then
            controlstring = "rdlenoi,constant"
        ElseIf ComboBox9.SelectedItem = "File" Then
            controlstring = "rdlenoi,file"
        Else
            controlstring = "rdlenoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox10.SelectedItem = "Constant" Then
            controlstring = "rdzpoi,constant"
        ElseIf ComboBox10.SelectedItem = "File" Then
            controlstring = "rdzpoi,file"
        Else
            controlstring = "rdzpoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox11.SelectedItem = "Constant" Then
            controlstring = "rdzecoi,constant"
        ElseIf ComboBox11.SelectedItem = "File" Then
            controlstring = "rdzecoi,file"
        Else
            controlstring = "rdzecoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox12.SelectedItem = "Constant" Then
            controlstring = "rdzenoi,constant"
        ElseIf ComboBox12.SelectedItem = "File" Then
            controlstring = "rdzenoi,file"
        Else
            controlstring = "rdzenoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox13.SelectedItem = "Constant" Then
            controlstring = "rllpoi,constant"
        ElseIf ComboBox13.SelectedItem = "File" Then
            controlstring = "rllpoi,file"
        Else
            controlstring = "rllpoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox14.SelectedItem = "Constant" Then
            controlstring = "rllecoi,constant"
        ElseIf ComboBox14.SelectedItem = "File" Then
            controlstring = "rllecoi,file"
        Else
            controlstring = "rllecoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox15.SelectedItem = "Constant" Then
            controlstring = "rllenoi,constant"
        ElseIf ComboBox15.SelectedItem = "File" Then
            controlstring = "rllenoi,file"
        Else
            controlstring = "rllenoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox25.SelectedItem = "File" Then
            controlstring = "rllooi,file"
        ElseIf ComboBox25.SelectedItem = "None" Then
            controlstring = "rllooi,none"
        Else
            controlstring = "rllooi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox16.SelectedItem = "Constant" Then
            controlstring = "rlzpoi,constant"
        ElseIf ComboBox16.SelectedItem = "File" Then
            controlstring = "rlzpoi,file"
        Else
            controlstring = "rlzpoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox17.SelectedItem = "Constant" Then
            controlstring = "rlzecoi,constant"
        ElseIf ComboBox17.SelectedItem = "File" Then
            controlstring = "rlzecoi,file"
        Else
            controlstring = "rlzecoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox18.SelectedItem = "Constant" Then
            controlstring = "rlzenoi,constant"
        ElseIf ComboBox18.SelectedItem = "File" Then
            controlstring = "rlzenoi,file"
        Else
            controlstring = "rlzenoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox26.SelectedItem = "File" Then
            controlstring = "rlzooi,file"
        ElseIf ComboBox26.SelectedItem = "None" Then
            controlstring = "rlzooi,none"
        Else
            controlstring = "rlzooi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox19.SelectedItem = "Constant" Then
            controlstring = "airpoi,constant"
        ElseIf ComboBox19.SelectedItem = "File" Then
            controlstring = "airpoi,file"
        Else
            controlstring = "airpoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox20.SelectedItem = "Constant" Then
            controlstring = "airecoi,constant"
        ElseIf ComboBox20.SelectedItem = "File" Then
            controlstring = "airecoi,file"
        Else
            controlstring = "airecoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox21.SelectedItem = "Constant" Then
            controlstring = "airenoi,constant"
        ElseIf ComboBox21.SelectedItem = "File" Then
            controlstring = "airenoi,file"
        Else
            controlstring = "airenoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox22.SelectedItem = "Constant" Then
            controlstring = "seapoi,constant"
        ElseIf ComboBox22.SelectedItem = "File" Then
            controlstring = "seapoi,file"
        Else
            controlstring = "seapoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox23.SelectedItem = "Constant" Then
            controlstring = "seaecoi,constant"
        ElseIf ComboBox23.SelectedItem = "File" Then
            controlstring = "seaecoi,file"
        Else
            controlstring = "seaecoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox24.SelectedItem = "Constant" Then
            controlstring = "seaenoi,constant"
        ElseIf ComboBox24.SelectedItem = "File" Then
            controlstring = "seaenoi,file"
        Else
            controlstring = "seaenoi,"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox28.SelectedItem = "Full Day" Then
            controlstring = "rllcumeas,full"
        ElseIf ComboBox28.SelectedItem = "Busiest Hour" Then
            controlstring = "rllcumeas,busy"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "rllpkhd," & TextBox38.Text
        cfs.WriteLine(controlstring)
        If ComboBox29.SelectedItem = "Elasticity" Then
            controlstring = "rdzspdcalc,elasticity"
        ElseIf ComboBox29.SelectedItem = "S/F Curve" Then
            controlstring = "rdzspdcalc,sfcurve"
        End If
        cfs.WriteLine(controlstring)
        If ComboBox30.SelectedItem = "Constant" Then
            controlstring = "triprate,constant"
        ElseIf ComboBox30.SelectedItem = "Strategy File" Then
            controlstring = "triprate,Strategy"
        End If
        cfs.WriteLine(controlstring)
        If CheckBox43.Checked = True Then
            controlstring = "UpdateInput,True"
        Else
            controlstring = "UpdateInput,False"
        End If
        cfs.WriteLine(controlstring)
        controlstring = "StartYear," & TextBox39.Text
        cfs.WriteLine(controlstring)
        controlstring = "Duration," & TextBox40.Text
        cfs.WriteLine(controlstring)
        cfs.Close()
    End Sub

    Sub OpenControlFile()
        Dim cfo As IO.StreamReader
        Dim controlstring As String
        Dim controlarray() As String

        cfo = New IO.StreamReader(ControlFile, System.Text.Encoding.Default)
        controlstring = cfo.ReadLine
        Do Until controlstring Is Nothing
            controlarray = Split(controlstring, ",")
            Select Case controlarray(0)
                Case "roadlinkmodule"
                    If controlarray(1) = "True" Then
                        CheckBox1.Checked = True
                    Else
                        CheckBox1.Checked = False
                    End If
                Case "roadzonemodule"
                    If controlarray(1) = "True" Then
                        CheckBox2.Checked = True
                    Else
                        CheckBox2.Checked = False
                    End If
                Case "raillinkmodule"
                    If controlarray(1) = "True" Then
                        CheckBox3.Checked = True
                    Else
                        CheckBox3.Checked = False
                    End If
                Case "railzonemodule"
                    If controlarray(1) = "True" Then
                        CheckBox4.Checked = True
                    Else
                        CheckBox4.Checked = False
                    End If
                Case "airmodule"
                    If controlarray(1) = "True" Then
                        CheckBox5.Checked = True
                    Else
                        CheckBox5.Checked = False
                    End If
                Case "seamodule"
                    If controlarray(1) = "True" Then
                        CheckBox6.Checked = True
                    Else
                        CheckBox6.Checked = False
                    End If
                Case "modelfiles"
                    DirPath = controlarray(1)
                Case "outputprefix"
                    TextBox1.Text = controlarray(1)
                Case "extvarfilesource"
                    ListBox1.SelectedIndex = controlarray(1)
                Case "evprefix"
                    TextBox2.Text = controlarray(1)
                Case "newrdlfile"
                    If controlarray(1) = "True" Then
                        CheckBox7.Checked = True
                    Else
                        CheckBox7.Checked = False
                    End If
                Case "newrdzfile"
                    If controlarray(1) = "True" Then
                        CheckBox8.Checked = True
                    Else
                        CheckBox8.Checked = False
                    End If
                Case "newrllfile"
                    If controlarray(1) = "True" Then
                        CheckBox9.Checked = True
                    Else
                        CheckBox9.Checked = False
                    End If
                Case "newrlzfile"
                    If controlarray(1) = "True" Then
                        CheckBox10.Checked = True
                    Else
                        CheckBox10.Checked = False
                    End If
                Case "newairfile"
                    If controlarray(1) = "True" Then
                        CheckBox11.Checked = True
                    Else
                        CheckBox11.Checked = False
                    End If
                Case "newseafile"
                    If controlarray(1) = "True" Then
                        CheckBox12.Checked = True
                    Else
                        CheckBox12.Checked = False
                    End If
                Case "rdlsource"
                    If controlarray(1) = "constant" Then
                        ComboBox3.SelectedItem = "Scaling constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox3.SelectedItem = "Input file"
                    ElseIf controlarray(1) = "database" Then
                        ComboBox3.SelectedItem = "Database input"
                    End If
                Case "rdzsource"
                    If controlarray(1) = "constant" Then
                        ComboBox4.SelectedItem = "Scaling constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox4.SelectedItem = "Input file"
                    ElseIf controlarray(1) = "database" Then
                        ComboBox4.SelectedItem = "Database input"
                    End If
                Case "rllsource"
                    If controlarray(1) = "constant" Then
                        ComboBox2.SelectedItem = "Scaling constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox2.SelectedItem = "Input file"
                    ElseIf controlarray(1) = "database" Then
                        ComboBox2.SelectedItem = "Database input"
                    End If
                Case "rlzsource"
                    If controlarray(1) = "constant" Then
                        ComboBox1.SelectedItem = "Scaling constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox1.SelectedItem = "Input file"
                    ElseIf controlarray(1) = "database" Then
                        ComboBox1.SelectedItem = "Database input"
                    End If
                Case "airsource"
                    If controlarray(1) = "constant" Then
                        ComboBox5.SelectedItem = "Scaling constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox5.SelectedItem = "Input file"
                    ElseIf controlarray(1) = "database" Then
                        ComboBox5.SelectedItem = "Database input"
                    End If
                Case "seasource"
                    If controlarray(1) = "constant" Then
                        ComboBox6.SelectedItem = "Scaling constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox6.SelectedItem = "Input file"
                    ElseIf controlarray(1) = "database" Then
                        ComboBox6.SelectedItem = "Database input"
                    End If
                Case "newevprefix"
                    TextBox3.Text = controlarray(1)
                Case "newrdlinfra"
                    If controlarray(1) = "True" Then
                        CheckBox13.Checked = True
                    Else
                        CheckBox13.Checked = False
                    End If
                Case "newrdzinfra"
                    If controlarray(1) = "True" Then
                        CheckBox14.Checked = True
                    Else
                        CheckBox14.Checked = False
                    End If
                Case "newrllinfra"
                    If controlarray(1) = "True" Then
                        CheckBox15.Checked = True
                    Else
                        CheckBox15.Checked = False
                    End If
                Case "newrlzinfra"
                    If controlarray(1) = "True" Then
                        CheckBox16.Checked = True
                    Else
                        CheckBox16.Checked = False
                    End If
                Case "newairinfra"
                    If controlarray(1) = "True" Then
                        CheckBox17.Checked = True
                    Else
                        CheckBox17.Checked = False
                    End If
                Case "newseainfra"
                    If controlarray(1) = "True" Then
                        CheckBox18.Checked = True
                    Else
                        CheckBox18.Checked = False
                    End If
                Case "addinfraprefix"
                    TextBox4.Text = controlarray(1)
                Case "addrdlinfra"
                    If controlarray(1) = "True" Then
                        CheckBox19.Checked = True
                    Else
                        CheckBox19.Checked = False
                    End If
                Case "addrdzinfra"
                    If controlarray(1) = "True" Then
                        CheckBox20.Checked = True
                    Else
                        CheckBox20.Checked = False
                    End If
                Case "addrllinfra"
                    If controlarray(1) = "True" Then
                        CheckBox21.Checked = True
                    Else
                        CheckBox21.Checked = False
                    End If
                Case "addrlzinfra"
                    If controlarray(1) = "True" Then
                        CheckBox22.Checked = True
                    Else
                        CheckBox22.Checked = False
                    End If
                Case "addairinfra"
                    If controlarray(1) = "True" Then
                        CheckBox23.Checked = True
                    Else
                        CheckBox23.Checked = False
                    End If
                Case "addseainfra"
                    If controlarray(1) = "True" Then
                        CheckBox24.Checked = True
                    Else
                        CheckBox24.Checked = False
                    End If
                Case "newinfraprefix"
                    TextBox5.Text = controlarray(1)
                Case "popdincheck"
                    If controlarray(1) = "True" Then
                        CheckBox25.Checked = True
                    Else
                        CheckBox25.Checked = False
                    End If
                Case "popgincheck"
                    If controlarray(1) = "True" Then
                        CheckBox28.Checked = True
                    Else
                        CheckBox28.Checked = False
                    End If
                Case "ecoincheck"
                    If controlarray(1) = "True" Then
                        CheckBox26.Checked = True
                    Else
                        CheckBox26.Checked = False
                    End If
                Case "eneincheck"
                    If controlarray(1) = "True" Then
                        CheckBox27.Checked = True
                    Else
                        CheckBox27.Checked = False
                    End If
                Case "popdfile"
                    TextBox6.Text = controlarray(1)
                Case "popgfile"
                    TextBox9.Text = controlarray(1)
                Case "ecofile"
                    TextBox7.Text = controlarray(1)
                Case "enefile"
                    TextBox8.Text = controlarray(1)
                Case "rdlpoi"
                    If controlarray(1) = "constant" Then
                        ComboBox7.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox7.SelectedItem = "File"
                    End If
                Case "rdlecoi"
                    If controlarray(1) = "constant" Then
                        ComboBox8.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox8.SelectedItem = "File"
                    End If
                Case "rdlenoi"
                    If controlarray(1) = "constant" Then
                        ComboBox9.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox9.SelectedItem = "File"
                    End If
                Case "rdzpoi"
                    If controlarray(1) = "constant" Then
                        ComboBox10.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox10.SelectedItem = "File"
                    End If
                Case "rdzecoi"
                    If controlarray(1) = "constant" Then
                        ComboBox11.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox11.SelectedItem = "File"
                    End If
                Case "rdzenoi"
                    If controlarray(1) = "constant" Then
                        ComboBox12.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox12.SelectedItem = "File"
                    End If
                Case "rllpoi"
                    If controlarray(1) = "constant" Then
                        ComboBox13.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox13.SelectedItem = "File"
                    End If
                Case "rllecoi"
                    If controlarray(1) = "constant" Then
                        ComboBox14.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox14.SelectedItem = "File"
                    End If
                Case "rllenoi"
                    If controlarray(1) = "constant" Then
                        ComboBox15.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox15.SelectedItem = "File"
                    End If
                Case "rllooi"
                    If controlarray(1) = "file" Then
                        ComboBox25.SelectedItem = "File"
                    ElseIf controlarray(1) = "none" Then
                        ComboBox25.SelectedItem = "None"
                    End If
                Case "rlzpoi"
                    If controlarray(1) = "constant" Then
                        ComboBox16.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox16.SelectedItem = "File"
                    End If
                Case "rlzecoi"
                    If controlarray(1) = "constant" Then
                        ComboBox17.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox17.SelectedItem = "File"
                    End If
                Case "rlzenoi"
                    If controlarray(1) = "constant" Then
                        ComboBox18.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox18.SelectedItem = "File"
                    End If
                Case "rlzooi"
                    If controlarray(1) = "file" Then
                        ComboBox26.SelectedItem = "File"
                    ElseIf controlarray(1) = "none" Then
                        ComboBox26.SelectedItem = "None"
                    End If
                Case "airpoi"
                    If controlarray(1) = "constant" Then
                        ComboBox19.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox19.SelectedItem = "File"
                    End If
                Case "airecoi"
                    If controlarray(1) = "constant" Then
                        ComboBox20.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox20.SelectedItem = "File"
                    End If
                Case "airenoi"
                    If controlarray(1) = "constant" Then
                        ComboBox21.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox21.SelectedItem = "File"
                    End If
                Case "seapoi"
                    If controlarray(1) = "constant" Then
                        ComboBox22.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox22.SelectedItem = "File"
                    End If
                Case "seaecoi"
                    If controlarray(1) = "constant" Then
                        ComboBox23.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox23.SelectedItem = "File"
                    End If
                Case "seaenoi"
                    If controlarray(1) = "constant" Then
                        ComboBox24.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "file" Then
                        ComboBox24.SelectedItem = "File"
                    End If
                Case "SubStrategy"
                    If controlarray(1) = "0" Then
                        ComboBox27.SelectedItem = "TR0: Decline and Decay"
                    ElseIf controlarray(1) = "1" Then
                        ComboBox27.SelectedItem = "TR1: Predict and Provide"
                    ElseIf controlarray(1) = "2" Then
                        ComboBox27.SelectedItem = "TR2: Cost and Constrain"
                    ElseIf controlarray(1) = "3" Then
                        ComboBox27.SelectedItem = "TR3: Adapting the Fleet"
                    ElseIf controlarray(1) = "4" Then
                        ComboBox27.SelectedItem = "TR4: Promo-Pricing"
                    ElseIf controlarray(1) = "5" Then
                        ComboBox27.SelectedItem = "TR5: Connected Grid"
                    ElseIf controlarray(1) = "6" Then
                        ComboBox27.SelectedItem = "TR6: Smarter Choices"
                    End If
                Case "autoinfra"
                    If controlarray(1) = "True" Then
                        CheckBox29.Checked = True
                    Else
                        CheckBox29.Checked = False
                    End If
                Case "infraper"
                    TextBox10.Text = controlarray(1)
                Case "varel"
                    If controlarray(1) = "True" Then
                        CheckBox30.Checked = True
                    Else
                        CheckBox30.Checked = False
                    End If
                Case "varelrat"
                    TextBox11.Text = controlarray(1)
                Case "rdconcharge"
                    If controlarray(1) = "True" Then
                        CheckBox31.Checked = True
                    Else
                        CheckBox31.Checked = False
                    End If
                Case "rdconcper"
                    TextBox12.Text = controlarray(1)
                Case "rdemcharge"
                    If controlarray(1) = "True" Then
                        CheckBox32.Checked = True
                    Else
                        CheckBox32.Checked = False
                    End If
                Case "rdwppl"
                    If controlarray(1) = "True" Then
                        CheckBox33.Checked = True
                    Else
                        CheckBox33.Checked = False
                    End If
                Case "wpplyear"
                    TextBox13.Text = controlarray(1)
                Case "wpplper"
                    TextBox14.Text = controlarray(1)
                Case "rlconcharge"
                    If controlarray(1) = "True" Then
                        CheckBox34.Checked = True
                    Else
                        CheckBox34.Checked = False
                    End If
                Case "rlconcper"
                    TextBox15.Text = controlarray(1)
                Case "airconcharge"
                    If controlarray(1) = "True" Then
                        CheckBox35.Checked = True
                    Else
                        CheckBox35.Checked = False
                    End If
                Case "airconcper"
                    TextBox16.Text = controlarray(1)
                Case "railelec"
                    If controlarray(1) = "True" Then
                        CheckBox36.Checked = True
                    Else
                        CheckBox36.Checked = False
                    End If
                Case "railelkm"
                    TextBox17.Text = controlarray(1)
                Case "scotwaldisagg"
                    If controlarray(1) = "True" Then
                        CheckBox37.Checked = True
                    Else
                        CheckBox37.Checked = False
                    End If
                Case "smartchoice"
                    If controlarray(1) = "True" Then
                        CheckBox38.Checked = True
                    Else
                        CheckBox38.Checked = False
                    End If
                Case "smartchoiceintro"
                    TextBox18.Text = controlarray(1)
                Case "smartchoiceper"
                    TextBox19.Text = controlarray(1)
                Case "smartchoiceyears"
                    TextBox20.Text = controlarray(1)
                Case "rdemchyr"
                    TextBox27.Text = controlarray(1)
                Case "rdconcyear"
                    TextBox28.Text = controlarray(1)
                Case "rlconcyear"
                    TextBox29.Text = controlarray(1)
                Case "rlemcharge"
                    If controlarray(1) = "True" Then
                        CheckBox41.Checked = True
                    Else
                        CheckBox41.Checked = False
                    End If
                Case "rlemchyr"
                    TextBox30.Text = controlarray(1)
                Case "airconcyr"
                    TextBox31.Text = controlarray(1)
                Case "smartlogistics"
                    If controlarray(1) = "True" Then
                        CheckBox39.Checked = True
                    Else
                        CheckBox39.Checked = False
                    End If
                Case "smartlogintro"
                    TextBox23.Text = controlarray(1)
                Case "smartlogper"
                    TextBox22.Text = controlarray(1)
                Case "smartlogyears"
                    TextBox21.Text = controlarray(1)
                Case "urbfrtinn"
                    If controlarray(1) = "True" Then
                        CheckBox40.Checked = True
                    Else
                        CheckBox40.Checked = False
                    End If
                Case "urbfrtinnintro"
                    TextBox26.Text = controlarray(1)
                Case "urbfrtinnper"
                    TextBox25.Text = controlarray(1)
                Case "urbfrtinnyears"
                    TextBox24.Text = controlarray(1)
                Case "airemcharge"
                    If controlarray(1) = "True" Then
                        CheckBox42.Checked = True
                    Else
                        CheckBox42.Checked = False
                    End If
                Case "airemcyr"
                    TextBox32.Text = controlarray(1)
                Case "newseaton"
                    TextBox33.Text = controlarray(1)
                Case "newrdlanes"
                    TextBox34.Text = controlarray(1)
                Case "rltrackperyr"
                    TextBox35.Text = controlarray(1)
                Case "airrunperyr"
                    TextBox36.Text = controlarray(1)
                Case "airtermperyr"
                    TextBox37.Text = controlarray(1)
                Case "rllcumeas"
                    If controlarray(1) = "full" Then
                        ComboBox28.SelectedItem = "Full Day"
                    ElseIf controlarray(1) = "busy" Then
                        ComboBox28.SelectedItem = "Busiest Hour"
                    End If
                Case "rllpkhd"
                    TextBox38.Text = controlarray(1)
                Case "rdzspdcalc"
                    If controlarray(1) = "elasticity" Then
                        ComboBox29.SelectedItem = "Elasticity"
                    ElseIf controlarray(1) = "sfcurve" Then
                        ComboBox29.SelectedItem = "S/F Curve"
                    End If
                Case "triprate"
                    If controlarray(1) = "constant" Then
                        ComboBox30.SelectedItem = "Constant"
                    ElseIf controlarray(1) = "Strategy" Then
                        ComboBox30.SelectedItem = "Strategy File"
                    End If
                Case "UpdateInput"
                    If controlarray(1) = "True" Then
                        CheckBox43.Checked = True
                    Else
                        CheckBox43.Checked = False
                    End If
                Case "StartYear"
                    TextBox39.Text = controlarray(1)
                Case "Duration"
                    TextBox40.Text = controlarray(1)
            End Select
            controlstring = cfo.ReadLine
        Loop
        cfo.Close()
    End Sub

    Private Sub ComboBox27_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox27.SelectedIndexChanged
        If ComboBox27.SelectedItem = "TR0: Decline and Decay" Then
            SubStrategy = 0
        ElseIf ComboBox27.SelectedItem = "TR1: Predict and Provide" Then
            SubStrategy = 1
        ElseIf ComboBox27.SelectedItem = "TR2: Cost and Constrain" Then
            SubStrategy = 2
        ElseIf ComboBox27.SelectedItem = "TR3: Adapting the Fleet" Then
            SubStrategy = 3
        ElseIf ComboBox27.SelectedItem = "TR4: Promo-Pricing" Then
            SubStrategy = 4
        ElseIf ComboBox27.SelectedItem = "TR5: Connected Grid" Then
            SubStrategy = 5
        ElseIf ComboBox27.SelectedItem = "TR6: Smarter Choices" Then
            SubStrategy = 6
        Else
            SubStrategy = 7
        End If
    End Sub

    Private Sub TextBox10_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox10.TextChanged
        Dim validval As Boolean
        If CDbl(TextBox10.Text) >= 0 Then
            If CDbl(TextBox10.Text) <= 100 Then
                validval = True
                CUCritValue = CDbl(TextBox10.Text) / 100
            Else
                validval = False
            End If
        Else
            validval = False
        End If
        If validval = False Then
            MsgBox("The critical capacity utilisation value must be between 0 and 100%.  Please insert a valid value.")
        End If
    End Sub

    Private Sub CheckBox29_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox29.CheckedChanged
        If CheckBox29.Checked = True Then
            BuildInfra = True
            TextBox10.Enabled = True
            Label15.Enabled = True
        Else
            BuildInfra = False
            TextBox10.Enabled = False
            Label15.Enabled = False
        End If
    End Sub

    Private Sub CheckBox30_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox30.CheckedChanged
        If CheckBox30.Checked = True Then
            TextBox11.Enabled = True
            Label16.Enabled = True
            VariableEl = True
        Else
            TextBox11.Enabled = False
            Label16.Enabled = False
            VariableEl = False
        End If
    End Sub

    Private Sub TextBox11_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox11.TextChanged
        Dim validval As Boolean
        If CDbl(TextBox11.Text) >= 0 Then
            validval = True
            ElCritValue = CDbl(TextBox11.Text) / 100
        Else
            validval = False
            MsgBox("The critical ratio must be a number greater than or equal to 0.  Please insert a valid value.")
        End If
    End Sub

    Private Sub CheckBox31_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox31.CheckedChanged
        If CheckBox31.Checked = True Then
            CongestionCharge = True
            TextBox12.Enabled = True
            Label17.Enabled = True
            TextBox28.Enabled = True
            Label33.Enabled = True
        Else
            CongestionCharge = False
            TextBox12.Enabled = False
            Label17.Enabled = False
            TextBox28.Enabled = False
            Label33.Enabled = False
        End If
    End Sub

    Private Sub TextBox12_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox12.TextChanged
        Dim validval As Boolean
        If CDbl(TextBox12.Text) >= 0 Then
            validval = True
            ConChargePer = CDbl(TextBox12.Text) / 100
        Else
            validval = False
            MsgBox("The percentage of standing costs must be a number greater than or equal to 0.  Please insert a valid value.")
        End If
    End Sub

    Private Sub CheckBox32_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox32.CheckedChanged
        If CheckBox32.Checked = True Then
            CarbonCharge = True
            TextBox27.Enabled = True
            Label32.Enabled = True
        Else
            CarbonCharge = False
            TextBox27.Enabled = False
            Label32.Enabled = False
        End If
    End Sub

    Private Sub CheckBox33_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox33.CheckedChanged
        If CheckBox33.Checked = True Then
            WPPL = True
            TextBox13.Enabled = True
            TextBox14.Enabled = True
            Label18.Enabled = True
            Label19.Enabled = True
        Else
            WPPL = False
            TextBox13.Enabled = False
            TextBox14.Enabled = False
            Label18.Enabled = False
            Label19.Enabled = False
        End If
    End Sub

    Private Sub TextBox13_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox13.TextChanged
        WPPLYear = TextBox13.Text
    End Sub

    Private Sub TextBox14_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox14.TextChanged
        WPPLPer = TextBox14.Text
    End Sub

    Private Sub CheckBox34_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox34.CheckedChanged
        If CheckBox34.Checked = True Then
            RailCCharge = True
            TextBox15.Enabled = True
            Label20.Enabled = True
            TextBox29.Enabled = True
            Label34.Enabled = True
        Else
            RailCCharge = False
            TextBox15.Enabled = False
            Label20.Enabled = False
            TextBox29.Enabled = False
            Label34.Enabled = False
        End If
    End Sub

    Private Sub TextBox15_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox15.TextChanged
        Dim validval As Boolean
        If CDbl(TextBox15.Text) >= 0 Then
            validval = True
            RailChargePer = CDbl(TextBox15.Text) / 100
        Else
            validval = False
            MsgBox("The percentage of non-variable costs must be a number greater than or equal to 0.  Please insert a valid value.")
        End If
    End Sub

    Private Sub CheckBox35_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox35.CheckedChanged
        If CheckBox35.Checked = True Then
            AirCCharge = True
            TextBox16.Enabled = True
            Label21.Enabled = True
            TextBox31.Enabled = True
            Label36.Enabled = True
        Else
            AirCCharge = False
            TextBox16.Enabled = False
            Label21.Enabled = False
            TextBox31.Enabled = False
            Label36.Enabled = False
        End If
    End Sub

    Private Sub TextBox16_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox16.TextChanged
        Dim validval As Boolean
        If CDbl(TextBox16.Text) >= 0 Then
            validval = True
            AirChargePer = CDbl(TextBox16.Text) / 100
        Else
            validval = False
            MsgBox("The percentage of non-fuel costs must be a number greater than or equal to 0.  Please insert a valid value.")
        End If
    End Sub

    Private Sub CheckBox36_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox36.CheckedChanged
        If CheckBox36.Checked = True Then
            RlElect = True
            TextBox17.Enabled = True
            Label22.Enabled = True
        Else
            RlElect = False
            TextBox17.Enabled = False
            Label22.Enabled = False
        End If
    End Sub

    Private Sub TextBox17_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox17.TextChanged
        If CDbl(TextBox17.Text) >= 0 Then
            ElectKmPerYear = CDbl(TextBox17.Text)
        Else
            MsgBox("The rail track km electrified per year must be a number greater than or equal to 0.  Please insert a valid value.")
        End If
    End Sub

    Private Sub CheckBox37_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox37.CheckedChanged
        If CheckBox37.Checked = True Then
            SWDisagg = True
        Else
            SWDisagg = False
        End If
    End Sub

    Private Sub CheckBox38_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox38.CheckedChanged
        If CheckBox38.Checked = True Then
            SmarterChoices = True
            TextBox18.Enabled = True
            TextBox19.Enabled = True
            TextBox20.Enabled = True
            Label23.Enabled = True
            Label24.Enabled = True
            Label25.Enabled = True
        Else
            SmarterChoices = False
            TextBox18.Enabled = False
            TextBox19.Enabled = False
            TextBox20.Enabled = False
            Label23.Enabled = False
            Label24.Enabled = False
            Label25.Enabled = False
        End If
    End Sub

    Private Sub TextBox18_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox18.TextChanged
        SmartIntro = CLng(TextBox18.Text) - 2010
    End Sub

    Private Sub TextBox19_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox19.TextChanged
        Dim validval As Boolean
        If CDbl(TextBox19.Text) >= 0 Then
            If CDbl(TextBox19.Text) <= 100 Then
                validval = True
                SmartPer = CDbl(TextBox19.Text) / 100
            Else
                validval = False
            End If
        Else
            validval = False
        End If
        If validval = False Then
            MsgBox("The percentage reduction in car trips must be between 0 and 100%.  Please insert a valid value.")
        End If
    End Sub

    Private Sub TextBox20_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox20.TextChanged
        If CDbl(TextBox20.Text) >= 1 Then
            SmartYears = CDbl(TextBox20.Text)
        Else
            MsgBox("The number of years to take full effect must be greater than or equal to 1.  Please insert a valid value.")
        End If
    End Sub

    Private Sub CheckBox39_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox39.CheckedChanged
        If CheckBox39.Checked = True Then
            SmartFrt = True
            TextBox21.Enabled = True
            TextBox22.Enabled = True
            TextBox23.Enabled = True
            Label26.Enabled = True
            Label27.Enabled = True
            Label28.Enabled = True
        Else
            SmartFrt = False
            TextBox21.Enabled = False
            TextBox22.Enabled = False
            TextBox23.Enabled = False
            Label26.Enabled = False
            Label27.Enabled = False
            Label28.Enabled = False
        End If
    End Sub

    Private Sub TextBox23_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox23.TextChanged
        SmFrtIntro = CLng(TextBox23.Text) - 2010
    End Sub

    Private Sub TextBox22_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox22.TextChanged
        Dim validval As Boolean
        If CDbl(TextBox22.Text) >= 0 Then
            If CDbl(TextBox22.Text) <= 100 Then
                validval = True
                SmFrtPer = CDbl(TextBox22.Text) / 100
            Else
                validval = False
            End If
        Else
            validval = False
        End If
        If validval = False Then
            MsgBox("The percentage reduction in interzonal road freight must be between 0 and 100%.  Please insert a valid value.")
        End If
    End Sub

    Private Sub TextBox21_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox21.TextChanged
        If CDbl(TextBox21.Text) >= 1 Then
            SmFrtYears = CDbl(TextBox21.Text)
        Else
            MsgBox("The number of years to take full effect must be greater than or equal to 1.  Please insert a valid value.")
        End If
    End Sub

    Private Sub CheckBox40_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox40.CheckedChanged
        If CheckBox40.Checked = True Then
            UrbanFrt = True
            TextBox26.Enabled = True
            TextBox25.Enabled = True
            TextBox24.Enabled = True
            Label31.Enabled = True
            Label30.Enabled = True
            Label29.Enabled = True
        Else
            UrbanFrt = False
            TextBox26.Enabled = False
            TextBox25.Enabled = False
            TextBox24.Enabled = False
            Label31.Enabled = False
            Label30.Enabled = False
            Label29.Enabled = False
        End If
    End Sub

    Private Sub TextBox26_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox26.TextChanged
        UrbFrtIntro = CLng(TextBox26.Text) - 2010
    End Sub

    Private Sub TextBox25_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox25.TextChanged
        Dim validval As Boolean
        If CDbl(TextBox25.Text) >= 0 Then
            If CDbl(TextBox25.Text) <= 100 Then
                validval = True
                UrbFrtPer = CDbl(TextBox25.Text) / 100
            Else
                validval = False
            End If
        Else
            validval = False
        End If
        If validval = False Then
            MsgBox("The percentage reduction in urban road freight must be between 0 and 100%.  Please insert a valid value.")
        End If
    End Sub

    Private Sub TextBox24_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox24.TextChanged
        If CDbl(TextBox24.Text) >= 1 Then
            UrbFrtYears = CDbl(TextBox24.Text)
        Else
            MsgBox("The number of years to take full effect must be greater than or equal to 1.  Please insert a valid value.")
        End If
    End Sub

    Private Sub TextBox27_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox27.TextChanged
        CarbChargeYear = CLng(TextBox27.Text) - 2010
    End Sub

    Private Sub TextBox28_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox28.TextChanged
        ConChargeYear = CLng(TextBox28.Text) - 2010
    End Sub

    Private Sub TextBox29_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox29.TextChanged
        RlCChargeYear = CLng(TextBox29.Text) - 2010
    End Sub

    Private Sub CheckBox41_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox41.CheckedChanged
        If CheckBox41.Checked = True Then
            RlCaCharge = True
            TextBox30.Enabled = True
            Label35.Enabled = True
        Else
            RlCaCharge = False
            TextBox30.Enabled = False
            Label35.Enabled = False
        End If
    End Sub

    Private Sub TextBox30_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox30.TextChanged
        RlCaChYear = CLng(TextBox30.Text) - 2010
    End Sub

    Private Sub TextBox31_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox31.TextChanged
        AirChargeYear = CLng(TextBox31.Text) - 2010
    End Sub

    Private Sub CheckBox42_CheckedChanged(sender As System.Object, e As System.EventArgs) Handles CheckBox42.CheckedChanged
        If CheckBox42.Checked = True Then
            AirCaCharge = True
            TextBox32.Enabled = True
            Label37.Enabled = True
        Else
            AirCaCharge = False
            TextBox32.Enabled = False
            Label37.Enabled = False
        End If
    End Sub

    Private Sub TextBox32_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox32.TextChanged
        AirCaChYear = CLng(TextBox32.Text) - 2010
    End Sub

    Private Sub TextBox33_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox33.TextChanged
        NewSeaTonnes = TextBox33.Text
    End Sub

    Private Sub TextBox34_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox34.TextChanged
        NewRoadLanes = TextBox34.Text
    End Sub

    Private Sub TextBox35_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox35.TextChanged
        NewRailTracks = TextBox35.Text
    End Sub

    Private Sub TextBox36_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox36.TextChanged
        NewAirRun = TextBox36.Text
    End Sub

    Private Sub TextBox37_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox37.TextChanged
        NewAirTerm = TextBox37.Text
    End Sub

    Private Sub ComboBox28_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox28.SelectedIndexChanged
        If ComboBox28.SelectedItem = "Full Day" Then
            RailCUPeriod = ""
            TextBox38.Enabled = False
            Label44.Enabled = False
        ElseIf ComboBox28.SelectedItem = "Busiest Hour" Then
            RailCUPeriod = "Hour"
            TextBox38.Enabled = True
            Label44.Enabled = True
        End If
    End Sub

    Private Sub TextBox38_TextChanged(sender As System.Object, e As System.EventArgs) Handles TextBox38.TextChanged
        If CDbl(TextBox38.Text) > 0 Then
            RlPeakHeadway = CDbl(TextBox38.Text)
        Else
            MsgBox("The peak headway must be a number greater than 0.  Please insert a valid value.")
        End If
    End Sub

    Private Sub ComboBox29_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox29.SelectedIndexChanged
        If ComboBox29.SelectedItem = "Elasticity" Then
            RdZSpdSource = "Elasticity"
        ElseIf ComboBox29.SelectedItem = "S/F Curve" Then
            RdZSpdSource = "Curve"
        End If
    End Sub

    Private Sub ComboBox30_SelectedIndexChanged(sender As System.Object, e As System.EventArgs) Handles ComboBox30.SelectedIndexChanged
        If ComboBox30.SelectedItem = "Constant" Then
            TripRates = "Constant"
        ElseIf ComboBox30.SelectedItem = "Strategy File" Then
            TripRates = "SubStrategy"
        End If
    End Sub


    Private Sub TextBox40_TextChanged(sender As Object, e As EventArgs) Handles TextBox40.TextChanged
        Duration = CInt(TextBox40.Text) - 1
        If StartYear + Duration > 90 Then
            MsgBox("Maximum duration is 90 years")
        ElseIf Duration < 1 Then
            MsgBox("Duration must between 1 to 90 years !")
        End If
    End Sub

    Private Sub TextBox39_TextChanged(sender As Object, e As EventArgs) Handles TextBox39.TextChanged
        StartYear = CInt(TextBox39.Text) - 2010 + 1
    End Sub

    Private Sub CheckBox43_CheckedChanged(sender As Object, e As EventArgs) Handles CheckBox43.CheckedChanged
        If CheckBox43.Checked = True Then
            UpdateInput = True
        Else
            UpdateInput = False
        End If
    End Sub

    Private Sub MainForm_Load(sender As Object, e As EventArgs) Handles MyBase.Load

    End Sub
End Class
