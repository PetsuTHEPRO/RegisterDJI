package com.sloth.registerapp.presentation.auth.screens

import android.util.Base64
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavController
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.auth.model.ServerAuthState
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.auth.SessionManager
import com.sloth.registerapp.features.auth.data.remote.dto.LoginRequestDto
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(
    navController: NavController,
    onRegisterClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    var username by remember { mutableStateOf("") } // Alterado de email para username
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Adicionado: Contexto e CoroutineScope
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tag = "LoginScreen"

    val colorScheme = MaterialTheme.colorScheme

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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo e Título
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Ícone do Drone com pulse effect
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "🚁",
                            fontSize = 40.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Título
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

            // Card do Formulário
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Username Field
                    OutlinedTextField(
                        value = username, // Alterado de email para username
                        onValueChange = {
                            username = it // Alterado de email para username
                            errorMessage = ""
                        },
                        label = { Text("Usuário", color = colorScheme.onSurfaceVariant) }, // Alterado de Email para Usuário
                        placeholder = { Text("seu_usuario", color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }, // Alterado de seu@email.com para seu_usuario
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person, // Alterado de Email para Person
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // Alterado de Email para Text
                        singleLine = true
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = ""
                        },
                        label = { Text("Senha", color = colorScheme.onSurfaceVariant) },
                        placeholder = { Text("Digite sua senha", color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
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

                    // Link "Esqueceu a senha?"
                    TextButton(
                        onClick = { /* TODO: Implementar recuperação de senha */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Esqueceu a senha?",
                            color = colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Mensagem de erro
                    AnimatedVisibility(
                        visible = errorMessage.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = colorScheme.error,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botão de Login
                    Button(
                        onClick = {
                            when {
                                username.isBlank() -> errorMessage = "Por favor, digite seu nome de usuário" // Alterado de email para nome de usuário
                                // Validação de email removida
                                password.isBlank() -> errorMessage = "Por favor, digite sua senha"
                                else -> {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            // Instanciar dependências
                                            val apiService = RetrofitClient.getInstance(context)
                                            val tokenRepository = TokenRepository.getInstance(context)
                                            val sessionManager = SessionManager.getInstance(context)
                                            val localSessionManager = LocalSessionManager.getInstance(context)

                                            // 1) Tenta login via BODY (mesmo fluxo do web)
                                            val response = try {
                                                Log.d(tag, "Tentando login via body")
                                                apiService.loginWithBody(LoginRequestDto(username, password))
                                            } catch (e: HttpException) {
                                                if (e.code() == 401) {
                                                    // 2) Fallback: Basic Auth (compatibilidade com backend antigo)
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

                                            // Tenta atualizar dados com auth/me para consolidar identidade
                                            try {
                                                val userMe = apiService.getMe("Bearer $accessToken")
                                                if (effectiveUserId.isBlank()) {
                                                    effectiveUserId = userMe.id
                                                }
                                                effectiveUsername = userMe.username.ifBlank { effectiveUsername }
                                                effectiveEmail = userMe.email.orEmpty()
                                            } catch (e: Exception) {
                                                Log.w(tag, "auth/me falhou após login, mantendo sessão mínima.", e)
                                            }

                                            if (effectiveUserId.isBlank()) {
                                                // Evita estado inconsistente quando backend não envia user_id no login.
                                                effectiveUserId = "local:$username"
                                            }

                                            // Salvar sessão local primeiro para evitar corrida de estado no app.
                                            sessionManager.createSession(
                                                token = accessToken,
                                                userId = effectiveUserId,
                                                username = effectiveUsername,
                                                email = effectiveEmail,
                                                expiryDays = 7L // Token válido por 7 dias
                                            )

                                            // Salva access + refresh para renovação automática
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

                                            // Navegar para o dashboard
                                            navController.navigate("dashboard") {
                                                popUpTo("welcome") { inclusive = true }
                                            }

                                        } catch (e: HttpException) {
                                            Log.e(tag, "Login HTTP ${e.code()}: ${e.message()}")
                                            errorMessage = when (e.code()) {
                                                401 -> "Usuário ou senha inválidos." // Alterado de Email para Usuário
                                                else -> "Erro ${e.code()}: ${e.message()}"
                                            }
                                        } catch (e: Exception) {
                                            Log.e(tag, "Login error: ${e.message}", e)
                                            errorMessage = "Falha na conexão. Verifique sua internet."
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
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
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🚀",
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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
            }

            // Link para Registro
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Não tem uma conta?",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onRegisterClick) {
                    Text(
                        text = "Registre-se",
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            TextButton(
                onClick = onSkipClick,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Pular por agora",
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // Footer com informações
            Column(
                modifier = Modifier.padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.secondary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(colorScheme.secondary, RoundedCornerShape(50.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sistema Online",
                                color = colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "Autonomous System v1.0",
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
