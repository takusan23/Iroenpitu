package io.github.takusan23.iroenpitu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.iroenpitu.Preference
import io.github.takusan23.iroenpitu.preference
import kotlinx.coroutines.launch

/**
 * AWS の認証情報を入力するためのボトムシート
 *
 * @param onDismissRequest 消す要求が来た
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingBottomSheet(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settingMap = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(key1 = Unit) {
        preference.load().forEach { (key, value) -> settingMap[key] = value }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row {
                Text(
                    text = "初期設定",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    scope.launch { preference.update(settingMap) }
                    onDismissRequest()
                }) { Text(text = "保存") }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_ACCESS_KEY] ?: "",
                onValueChange = { settingMap[Preference.KEY_ACCESS_KEY] = it },
                label = { Text(text = "アクセスキー") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_SECRET_ACCESS_LEY] ?: "",
                onValueChange = { settingMap[Preference.KEY_SECRET_ACCESS_LEY] = it },
                label = { Text(text = "シークレットアクセスキー") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_REGION] ?: "",
                onValueChange = { settingMap[Preference.KEY_REGION] = it },
                label = { Text(text = "リージョン") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_INPUT_BUCKET] ?: "",
                onValueChange = { settingMap[Preference.KEY_INPUT_BUCKET] = it },
                label = { Text(text = "画像受け付け用バケット名") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_OUTPUT_BUCKET] ?: "",
                onValueChange = { settingMap[Preference.KEY_OUTPUT_BUCKET] = it },
                label = { Text(text = "画像配信用バケット名") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_OEKAKITYOU_BASE_URL] ?: "",
                onValueChange = { settingMap[Preference.KEY_OEKAKITYOU_BASE_URL] = it },
                label = { Text(text = "おえかきちょう ベース URL") }
            )
        }
    }
}
