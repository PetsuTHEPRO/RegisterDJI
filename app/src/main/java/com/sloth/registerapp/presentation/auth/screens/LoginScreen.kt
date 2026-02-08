package com.sloth.registerapp.presentation.auth.screens

import android.util.Base64
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.auth.SessionManager
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.auth.model.ServerAuthState
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.features.auth.data.manager.LoginHistoryManager
import com.sloth.registerapp.features.auth.data.remote.dto.LoginRequestDto
import com.sloth.registerapp.features.auth.domain.model.LoginAttemptStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(
    navController: NavController,
    onRegisterClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isApiOnline by remember { mutableStateOf<Boolean?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val loginHistoryManager = remember(context) { LoginHistoryManager.getInstance(context) }
    val tag = "LoginScreen"
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        val apiService = RetrofitClient.getInstance(context)
        while (isActive) {
            isApiOnline = runCatching {
                val health = apiService.getApiStatus()
                val text = "${health.status.orEmpty()} ${health.message.orEmpty()}".lowercase()
                health.running == true ||
                    text.contains("running") ||
                    text.contains("rodando") ||
                    text.contains("online") ||
                    text.contains("ok")
            }.getOrElse { false }
            delay(12000)
        }
    }

    fun showError(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.background,
                        colorScheme.surfaceVariant,
                        colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onSkipClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = colorScheme.surface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Vantly Neural",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Bem-vindo de volta",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f)),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuário", color = colorScheme.onSurfaceVariant) },
                        placeholder = {
                            Text(
                                "Digite seu usuário",
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline,
                            focusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            cursorColor = colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha", color = colorScheme.onSurfaceVariant) },
                        placeholder = {
                            Text(
                                "Digite sua senha",
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline,
                            focusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            cursorColor = colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )

                    TextButton(
                        onClick = { showError("Recuperação de senha ainda não implementada.") },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Esqueceu a senha?",
                            color = colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                username.isBlank() -> showError("Por favor, digite seu nome de usuário")
                                password.isBlank() -> showError("Por favor, digite sua senha")
                                else -> {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val apiService = RetrofitClient.getInstance(context)
                                            val tokenRepository = TokenRepository.getInstance(context)
                                            val sessionManager = SessionManager.getInstance(context)
                                            val localSessionManager = LocalSessionManager.getInstance(context)

                                            val response = try {
                                                Log.d(tag, "Tentando login via body")
                                                apiService.loginWithBody(LoginRequestDto(username, password))
                                            } catch (e: HttpException) {
                                                if (e.code() == 401) {
                                                    Log.d(tag, "Login via body falhou (401). Tentando Basic Auth")
                                                    val credentials = "$username:$password"
                                                    val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                                                    apiService.login(basicAuth)
                                                } else {
                                                    throw e
                                                }
                                            }

                                            val accessToken = response.resolvedAccessToken()
                                            if (accessToken.isNullOrBlank()) {
                                                throw IllegalStateException("Login sem access_token no payload")
                                            }

                                            var effectiveUserId = response.resolvedUserId()
                                            var effectiveUsername = username
                                            var effectiveEmail = ""

                                            try {
                                                val userMe = apiService.getMe("Bearer $accessToken")
                                                if (effectiveUserId.isBlank()) effectiveUserId = userMe.id
                                                effectiveUsername = userMe.username.ifBlank { effectiveUsername }
                                                effectiveEmail = userMe.email.orEmpty()
                                            } catch (e: Exception) {
                                                Log.w(tag, "auth/me falhou após login, mantendo sessão mínima.", e)
                                            }

                                            if (effectiveUserId.isBlank()) {
                                                effectiveUserId = "local:$username"
                                            }

                                            sessionManager.createSession(
                                                token = accessToken,
                                                userId = effectiveUserId,
                                                username = effectiveUsername,
                                                email = effectiveEmail,
                                                expiryDays = 7L
                                            )

                                            tokenRepository.saveTokens(
                                                accessToken = accessToken,
                                                refreshToken = response.refreshToken
                                            )

                                            localSessionManager.loginLocal(
                                                userId = effectiveUserId,
                                                username = effectiveUsername,
                                                email = effectiveEmail
                                            )
                                            localSessionManager.setServerAuthState(ServerAuthState.SERVER_AUTH_OK)
                                            loginHistoryManager.recordLoginAttempt(
                                                username = effectiveUsername.ifBlank { username },
                                                status = LoginAttemptStatus.SUCCESS,
                                                ownerUserId = effectiveUserId
                                            )

                                            navController.navigate("dashboard") {
                                                popUpTo("welcome") { inclusive = true }
                                            }
                                        } catch (e: HttpException) {
                                            Log.e(tag, "Login HTTP ${e.code()}: ${e.message()}")
                                            val message = if (e.code() == 401) {
                                                "Usuário ou senha inválidos."
                                            } else {
                                                "Erro ${e.code()}: ${e.message()}"
                                            }
                                            loginHistoryManager.recordLoginAttempt(
                                                username = username,
                                                status = LoginAttemptStatus.FAILED
                                            )
                                            showError(message)
                                        } catch (e: Exception) {
                                            Log.e(tag, "Login error: ${e.message}", e)
                                            loginHistoryManager.recordLoginAttempt(
                                                username = username,
                                                status = LoginAttemptStatus.FAILED
                                            )
                                            showError("Falha na conexão. Verifique sua internet.")
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        enabled = !isLoading,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(colorScheme.primary, colorScheme.primaryContainer)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = "Entrar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Não tem uma conta?",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.size(4.dp))
                TextButton(onClick = onRegisterClick) {
                    Text(
                        text = "Registre-se",
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val statusText = when (isApiOnline) {
                true -> "Sistema Online"
                false -> "Sistema Offline"
                null -> "Verificando sistema"
            }
            val statusColor = when (isApiOnline) {
                true -> colorScheme.secondary
                false -> colorScheme.error
                null -> colorScheme.tertiary
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, RoundedCornerShape(50.dp))
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
