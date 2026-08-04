package com.stacking.tracker.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoTexto(
    rotulo: String,
    valor: String,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null,
    sufixo: String? = null,
    apoio: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    linhaUnica: Boolean = true,
    minLinhas: Int = 1,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        modifier = modifier.fillMaxWidth(),
        label = { Text(rotulo, style = MaterialTheme.typography.bodySmall) },
        singleLine = linhaUnica,
        minLines = minLinhas,
        isError = erro != null,
        suffix = sufixo?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        supportingText = when {
            erro != null -> {
                { Text(erro, style = MaterialTheme.typography.bodySmall) }
            }
            apoio != null -> {
                { Text(apoio, style = MaterialTheme.typography.bodySmall) }
            }
            else -> null
        },
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = imeAction),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}
