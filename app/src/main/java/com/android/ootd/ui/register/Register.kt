package com.android.ootd.ui.register

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

object RegisterScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val INPUT_REGISTER_UNAME = "inputRegisterUname"
  const val INPUT_REGISTER_DATE = "inputRegisterDate"
  const val INPUT_REGISTER_LOCATION = "inputRegisterLocation"
  const val REGISTER_SAVE = "registerSave"
  const val ERROR_MESSAGE = "errorMessage"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(modelView: RegisterViewModel = viewModel(), onRegister: () -> Unit = {}) {
  val registerUiState by modelView.uiState.collectAsState()
  val errorMsg = registerUiState.errorMsg

  val context = LocalContext.current
  var touchedUserName by remember { mutableStateOf(false) }
  var touchedDate by remember { mutableStateOf(false) }
  var touchedLocation by remember { mutableStateOf(false) }

  val usernameError = touchedUserName && registerUiState.username.isBlank()
  // TODO    val dateError = touchedDate && signInUIState.date.isBlank()
  val anyError = usernameError // || dateError TODO add date

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      modelView.clearErrorMsg()
    }
  }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TODO:
            //            Image(
            //                painter = painterResource(id = R.drawable.image5),
            //                contentDescription = "Logo",
            //                contentScale = ContentScale.FillBounds,
            //                modifier = Modifier.width(250.dp).height(250.dp)
            //                    .testTag(SignInScreenTestTags.APP_LOGO)
            //            )
//            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = registerUiState.username,
                onValueChange = { modelView.setUsername(it) },
                label = { Text("Username") },
                placeholder = { Text("Enter your username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
//                    .onFocusChanged { focusState ->
//                        if (focusState.isFocused) touchedUserName = true }
                ,
                isError = usernameError
            )

//            if (usernameError) {
//                Text(
//                    text = "Please enter a valid username",
//                    color = Color.Red,
//                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.padding(8.dp).testTag(RegisterScreenTestTags.ERROR_MESSAGE)
//                )
//            }

            Spacer(modifier = Modifier.height(30.dp))

            // TODO:
            //          OutlinedTextField(
            //              value = signInUIState.username,
            //              onValueChange = {},
            //              label = { Text("Date of birth") },
            //              placeholder = { Text("DD/MM/YYYY") },
            //              modifier =
            //                  Modifier.fillMaxWidth()
            //                      .testTag(SignInScreenTestTags.INPUT_SIGNIN_DATE)
            //                      .onFocusChanged { focusState ->
            //                        if (focusState.isFocused) touchedDate = true
            //                      },
            //              isError = signInUIState.username.isBlank() && touchedDate)
            //          if (signInUIState.username.isBlank() && touchedDate) {
            //            Text(
            //                text = "Please enter a valid date",
            //                color = MaterialTheme.colorScheme.error,
            //                modifier =
            // Modifier.padding(8.dp).testTag(SignInScreenTestTags.ERROR_MESSAGE))
            //          }


            Button(
                onClick = {
                    modelView.registerUser()
                    onRegister()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RegisterScreenTestTags.REGISTER_SAVE),
                enabled = !anyError
            ) {
                Text("Save")
            }
        }
    }
}
