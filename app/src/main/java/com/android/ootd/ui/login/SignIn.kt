package com.android.ootd.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

object SignInScreenTestTags {
    const val APP_LOGO = "appLogo"
    const val INPUT_SIGNIN_UNAME = "inputSignInUname"
    const val INPUT_SIGNIN_DATE = "inputSignInDate"
    const val INPUT_SIGNIN_LOCATION = "inputSignInLocation"
    const val SIGNIN_SAVE = "signInSave"
    const val ERROR_MESSAGE = "errorMessage"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    modelView: SignInModelView = viewModel(),
    onRegister: () -> Unit = {}
){
    val signInUIState by modelView.uiState.collectAsState()
    val errorMsg = signInUIState.errorMsg

    val context = LocalContext.current
    var touchedUserName by remember { mutableStateOf(false) }
    var touchedDate by remember { mutableStateOf(false) }
    var touchedLocation by remember { mutableStateOf(false) }

    LaunchedEffect(errorMsg) {
        if (errorMsg != null) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            modelView.clearErrorMsg()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment =  Alignment.CenterHorizontally
        ) {
//            Image(
//                painter = painterResource(id = R.drawable.image5),
//                contentDescription = "Logo",
//                contentScale = ContentScale.FillBounds,
//                modifier = Modifier.width(250.dp).height(250.dp)
//                    .testTag(SignInScreenTestTags.APP_LOGO)
//            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = signInUIState.username,
                onValueChange = { modelView.setUsername(it) },
                label = { Text("Username") },
                placeholder = { Text("Enter your username") },
                modifier = Modifier.fillMaxWidth()
                    .testTag(SignInScreenTestTags.INPUT_SIGNIN_UNAME)
                    .onFocusChanged { focusState ->
                        if(focusState.isFocused) touchedUserName = true
                    },
                isError = signInUIState.username.isBlank() && touchedUserName
            )
            if(signInUIState.username.isBlank() && touchedUserName){
                Text(
                    text = "Please enter a valid username",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(8.dp)
                        .testTag(SignInScreenTestTags.ERROR_MESSAGE)
                )
            }
            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = signInUIState.username,
                onValueChange = {},
                label = { Text("Date of birth") },
                placeholder = { Text("DD/MM/YYYY") },
                modifier = Modifier.fillMaxWidth()
                    .testTag(SignInScreenTestTags.INPUT_SIGNIN_DATE)
                    .onFocusChanged { focusState ->
                        if(focusState.isFocused) touchedDate = true
                    },
                isError = signInUIState.username.isBlank() && touchedDate
            )
            if(signInUIState.username.isBlank() && touchedDate){
                Text(
                    text = "Please enter a valid date",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(8.dp)
                        .testTag(SignInScreenTestTags.ERROR_MESSAGE)
                )
            }

            Button(
                onClick = {
                    modelView.registerUser()
                    onRegister()
                },
                modifier = Modifier.fillMaxWidth().testTag(SignInScreenTestTags.SIGNIN_SAVE),
                enabled = signInUIState.username.isNotBlank()
            ){
                Text("Save")
            }

        }
    }

}