package com.sloth.registerapp.features.auth.presentation.login

import android.util.Base64
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
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.core.auth.TokenRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(
    navController: NavController,
    onRegisterClick: () -> Unit
) {
    var username by remember { mutableStateOf("") } // Alterado de email para username
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Adicionado: Contexto e CoroutineScope
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Cores do tema
    val primaryBlue = Color(0xFF3B82F6)
    val darkBlue = Color(0xFF1D4ED8)
    val lightBlue = Color(0xFF60A5FA)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)
    val textGray = Color(0xFF94A3B8)
    val textWhite = Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        darkBg,
                        Color(0xFF1A1F3A),
                        darkBg
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
                    color = primaryBlue.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.3f))
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
                    text = "Mission Control",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textWhite,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Bem-vindo de volta",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Card do Formulário
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = cardBg.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f)),
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
                        label = { Text("Usuário", color = textGray) }, // Alterado de Email para Usuário
                        placeholder = { Text("seu_usuario", color = textGray.copy(alpha = 0.5f)) }, // Alterado de seu@email.com para seu_usuario
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person, // Alterado de Email para Person
                                contentDescription = null,
                                tint = primaryBlue
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedContainerColor = darkBg.copy(alpha = 0.6f),
                            unfocusedContainerColor = darkBg.copy(alpha = 0.6f),
                            cursorColor = primaryBlue
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
                        label = { Text("Senha", color = textGray) },
                        placeholder = { Text("Digite sua senha", color = textGray.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = primaryBlue
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                    tint = textGray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedContainerColor = darkBg.copy(alpha = 0.6f),
                            unfocusedContainerColor = darkBg.copy(alpha = 0.6f),
                            cursorColor = primaryBlue
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
                            color = lightBlue,
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
                            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
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
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = Color(0xFFF87171),
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

                                            // Criar cabeçalho Basic Auth
                                            val credentials = "$username:$password" // Alterado de email para username
                                            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

                                            // Fazer a chamada de API
                                            val response = apiService.login(basicAuth)

                                            // Salvar o token
                                            tokenRepository.saveToken(response.token)

                                            // Navegar para o dashboard
                                            navController.navigate("dashboard") {
                                                popUpTo("welcome") { inclusive = true }
                                            }

                                        } catch (e: HttpException) {
                                            errorMessage = when (e.code()) {
                                                401 -> "Usuário ou senha inválidos." // Alterado de Email para Usuário
                                                else -> "Erro ${e.code()}: ${e.message()}"
                                            }
                                        } catch (e: Exception) {
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
                                        colors = listOf(primaryBlue, darkBlue)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
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
                                        color = Color.White
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
                    color = textGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onRegisterClick) {
                    Text(
                        text = "Registre-se",
                        color = lightBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
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
                        color = Color(0xFF22C55E).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF22C55E), RoundedCornerShape(50.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sistema Online",
                                color = Color(0xFF22C55E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "Autonomous System v1.0",
                    color = textGray.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}