package com.example.realm_benchmark

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.realm_benchmark.ui.theme.RealmbenchmarkTheme
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.internal.platform.createDefaultSystemLogger
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.log.RealmLog
import io.realm.kotlin.log.RealmLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import java.io.File
import kotlin.random.Random

class RealmActivity : ComponentActivity() {

    private val numberOfRows = MutableStateFlow(0)
    private val numberOfRows1 = MutableStateFlow(0)
    private var realm: Realm? = null
    private var entities: List<Entity0>? = null
    private var entities1: List<Entity1>? = null
    private var disposable: Disposable? = null
    private var observeJob: Job? = null
    private var observeJob1: Job? = null

    override fun onDestroy() {
        super.onDestroy()
        realm?.close()
        disposable?.dispose()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RealmbenchmarkTheme {
                val i by numberOfRows.collectAsState()
                val i2 by numberOfRows1.collectAsState()
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(text = "Realm Kotlin")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Number of rows ${i}, ${i2}")

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { openRealm() }) {
                        Text(text = "Open realm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { fillRealm() }) {
                        Text(text = "Fill realm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { writeRealm() }) {
                        Text(text = "Write realm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { observeRealm() }) {
                        Text(text = "Observe realm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { observeRealm2() }) {
                        Text(text = "Observe many")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { observeRealm3() }) {
                        Text(text = "Observe many (Flowable)")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { deleteRealm() }) {
                        Text(text = "Delete realm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { stopObserving() }) {
                        Text(text = "Stop observing")
                    }
                }

            }
        }
    }

    private fun openRealm() {
        if (this.realm != null) throw IllegalStateException("Realm already opened")
        val classes = setOf(
            Entity0::class,
            Entity1::class,
        )
        //RealmLog.setLevel(LogLevel.ALL)
        //RealmLog.add(createDefaultSystemLogger("!!!!!!!!"))
        val configuration = RealmConfiguration.Builder(classes)
            .schemaVersion(1)
            .directory(File(this.filesDir, "realm").absolutePath)
            .name("test.realm")
            .maxNumberOfActiveVersions(Long.MAX_VALUE)
            .build()

        val realm = Realm.open(configuration)
        this.realm = realm
        Log.d("!!!!!", "Realm opened")
    }

    private fun fillRealm() {
        val entities = (0..5000).map { Entity0(id = "$it", column1 = it) }
        val entities1 = (0..5000).map {
            Entity1(
                it,
                Random.nextLong().toString(),
                Random.nextLong().toString(),
                Random.nextLong().toString(),
                Random.nextLong().toString(),
                Random.nextLong().toString(),
                Random.nextLong().toString(),
                Random.nextLong().toString(),
                Random.nextLong().toString(),
                Random.nextLong().toString(),
            )
        }
        lifecycleScope.launch {
            realm!!.write {
                entities.forEach { copyToRealm(it, UpdatePolicy.ALL) }
                entities1.forEach { copyToRealm(it, UpdatePolicy.ALL) }
            }
            Log.d("!!!!!", "Realm filled")
        }
    }

    private fun observeRealm() {
        lifecycleScope.launch {
            realm!!.query(Entity0::class)
                .query("column1 >= $0", 2000)
                .asFlow()
                .collect {
                    Log.d("!!!!!", "Realm observed")
                    entities = it.list.toList()
                    numberOfRows.value = it.list.size
                }
        }
        lifecycleScope.launch {
            realm!!.query(Entity1::class)
                .query("id >= $0", 2000)
                .asFlow()
                .collect {
                    Log.d("!!!!!", "Realm observed")
                    entities1 = it.list.toList()
                    numberOfRows1.value = it.list.size
                }
        }
    }

    @SuppressLint("CheckResult")
    private fun observeRealm2() {
        observeJob = lifecycleScope.launch {
            val flows = (0..50).map { idx ->
                realm!!.query(Entity0::class)
                    .query("column1 >= $0", 4000 + idx * 100)
                    .asFlow()
                    .map { it.list }
            }
            combine(flows) { arr -> arr.toList().flatten() }
                .collect {
                    Log.d("!!!!!", "Realm observed many Entity0")
                    entities = it
                    numberOfRows.value = it.size
                }
        }
        observeJob1 = lifecycleScope.launch {
            val flows = (0..50).map { idx ->
                realm!!.query(Entity1::class)
                    .query("id >= $0", 4000 + idx * 100)
                    .asFlow()
                    .map { it.list }
            }
            combine(flows) { arr -> arr.toList().flatten() }
                .collect {
                    Log.d("!!!!!", "Realm observed many Entity1")
                    entities1 = it
                    numberOfRows1.value = it.size
                }
        }
    }

    private fun observeRealm3() {
        lifecycleScope.launch {
            val flowables = (0..30).map { idx ->
                realm!!.query(Entity0::class)
                    .query("column1 >= $0", 4000 + idx * 100)
                    .asFlow()
                    .map { it.list.toList() }
                    .asFlowable(Dispatchers.Main)
            }

            disposable = Flowable.combineLatest(flowables) { arr ->
                (arr.toList() as List<List<Entity0>>).flatten()
            }
                .subscribe {
                    entities = it.toList()
                    Log.d("!!!!!", "Realm observed many (Flowable)")
                    numberOfRows.value = it.size
                }

        }
    }

    private fun writeRealm() {
        lifecycleScope.launch {
            val entities =
                (0..1000).map { Entity0(id = "$it${Random.nextInt()}", column1 = 4000 + it) }
            lifecycleScope.launch {
                realm!!.write {
                    entities.forEach { copyToRealm(it, UpdatePolicy.ALL) }
                }
                Log.d("!!!!!", "Realm filled (1000 random)")
            }
        }
    }

    private fun deleteRealm() {
        lifecycleScope.launch {
            realm!!.write {
                deleteAll()
            }
            Log.d("!!!!!", "Realm deleted")
        }
    }

    private fun stopObserving() {
        observeJob?.cancel()
        observeJob1?.cancel()
        Log.d("!!!!!", "stopObserving")
    }
}
