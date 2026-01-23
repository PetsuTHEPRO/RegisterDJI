package com.sloth.registerapp.features.auth.presentation.register

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
import com.google.gson.Gson
import com.sloth.registerapp.features.auth.data.remote.dto.RegisterRequestDto
import com.sloth.registerapp.features.auth.data.remote.dto.RegisterResponseDto
import com.sloth.registerapp.core.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
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
                // Ícone do Drone
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
                    text = "Crie sua conta",
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
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = ""
                        },
                        label = { Text("Nome de Usuário", color = textGray) },
                        placeholder = { Text("Digite seu username", color = textGray.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
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
                        singleLine = true
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = ""
                        },
                        label = { Text("Email", color = textGray) },
                        placeholder = { Text("seu@email.com", color = textGray.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                        placeholder = { Text("Mínimo 6 caracteres", color = textGray.copy(alpha = 0.5f)) },
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

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = ""
                        },
                        label = { Text("Confirmar Senha", color = textGray) },
                        placeholder = { Text("Digite a senha novamente", color = textGray.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = primaryBlue
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar senha" else "Mostrar senha",
                                    tint = textGray
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                        singleLine = true,
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword
                    )

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

                    // Validação de senha
                    AnimatedVisibility(
                        visible = confirmPassword.isNotEmpty() && password != confirmPassword,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "⚠️ As senhas não coincidem",
                            color = Color(0xFFF87171),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botão de Registro
                    Button(
                        onClick = {
                            when {
                                username.isBlank() -> errorMessage = "Por favor, digite um nome de usuário"
                                email.isBlank() -> errorMessage = "Por favor, digite um email"
                                !email.contains("@") -> errorMessage = "Email inválido"
                                password.isBlank() -> errorMessage = "Por favor, digite uma senha"
                                password.length < 6 -> errorMessage = "A senha deve ter no mínimo 6 caracteres"
                                password != confirmPassword -> errorMessage = "As senhas não coincidem"
                                else -> {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val apiService = RetrofitClient.getInstance(context)
                                            val request = RegisterRequestDto(
                                                username = username.trim(),
                                                email = email.trim(),
                                                password = password
                                            )
                                            // Chamada de API
                                            apiService.register(request)
                                            // Sucesso, navega para o login
                                            onRegisterSuccess()

                                        } catch (e: HttpException) {
                                            errorMessage = if (e.code() == 400) {
                                                try { // Tenta parsear a mensagem de erro do backend
                                                    val errorBody = e.response()?.errorBody()?.string()
                                                    Gson().fromJson(errorBody, RegisterResponseDto::class.java).message
                                                } catch (jsonE: Exception) {
                                                    "Ocorreu um erro ao registrar." // Fallback
                                                }
                                            } else {
                                                "Erro ${e.code()}: Não foi possível registrar."
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
                                        text = "Criar Conta",
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

            // Link para Login
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Já tem uma conta?",
                    color = textGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Fazer Login",
                        color = lightBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }}