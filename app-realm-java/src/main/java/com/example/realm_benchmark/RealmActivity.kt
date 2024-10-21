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
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

@RealmModule(
    allClasses = false,
    library = true,
    classes = [
        Entity0::class,
        Entity1::class,
    ]
)
object RealmModule

class RealmActivity : ComponentActivity() {


    private val numberOfRows = MutableStateFlow(0)
    private val numberOfRows1 = MutableStateFlow(0)
    private var realm: Realm? = null
    private var entities: List<Entity0>? = null
    private var entities1: List<Entity1>? = null
    private var disposable: CompositeDisposable = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        realm?.close()
        disposable.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RealmbenchmarkTheme {
                // A surface container using the 'background' color from the theme
                val i by numberOfRows.collectAsState()
                val i2 by numberOfRows1.collectAsState()
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(text = "Realm Java")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Number of rows ${i}, ${i2}")

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { openRealm() } ) {
                        Text(text = "Open realm")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { fillRealm() } ) {
                        Text(text = "Fill realm")
                    }


                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { writeRealm() } ) {
                        Text(text = "Write realm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { observeRealm() } ) {
                        Text(text = "Observe realm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { observeRealm2() } ) {
                        Text(text = "Observe realm many")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { deleteRealm() } ) {
                        Text(text = "Delete realm")
                    }


                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {  } ) {
                        Text(text = "DUMP")
                    }
                }

            }
        }
    }

    private fun openRealm() {
        Realm.init(applicationContext)

        val configuration = RealmConfiguration.Builder()
            .schemaVersion(1)
            .modules(RealmModule)
            .directory(File(this.filesDir, "realm"))
            .name("test-java.realm")
            .maxNumberOfActiveVersions(Long.MAX_VALUE)
            .build()

        val realm = Realm.getInstance(configuration)
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
        lifecycleScope.launch(Dispatchers.IO) {
            val newRealm = Realm.getInstance(realm!!.configuration)
            newRealm.executeTransaction {
                it.copyToRealm(entities)
                it.copyToRealm(entities1)
            }
            newRealm.close()
            Log.d("!!!!!", "Realm filled")
        }
    }


    private fun writeRealm() {
        val entities = (0..1000).map { Entity0(id = "$it${Random.nextInt()}" , column1 = 4000 + it) }
        lifecycleScope.launch(Dispatchers.IO) {
            val newRealm = Realm.getInstance(realm!!.configuration)
            newRealm.executeTransaction {
                it.copyToRealm(entities)
            }
            newRealm.close()
            Log.d("!!!!!", "Realm filled (1000 random)")
        }
    }


    @SuppressLint("CheckResult")
    private fun observeRealm() {
        realm!!.where(Entity0::class.java)
            .greaterThanOrEqualTo("column1", 2000)
            .findAllAsync()
            .asFlowable()
            .filter { it.isLoaded }
            .subscribe {
                entities = it.toList()
                Log.d("!!!!!", "Realm observed")
                numberOfRows.value = it.size
            }
            .let { disposable.add(it) }

        realm!!.where(Entity1::class.java)
            .greaterThanOrEqualTo("id", 2000)
            .findAllAsync()
            .asFlowable()
            .filter { it.isLoaded }
            .subscribe {
                entities1 = it.toList()
                Log.d("!!!!!", "Realm observed")
                numberOfRows1.value = it.size
            }
            .let { disposable.add(it) }
    }

    @SuppressLint("CheckResult")
    private fun observeRealm2() {
        val flowables = (0..50).map { idx ->
            realm!!.where(Entity0::class.java)
                .greaterThanOrEqualTo("column1", 4000 + idx * 100)
                .findAllAsync()
                .asFlowable()
                .filter { it.isLoaded }
                .map { it.toList() }
        }
        Flowable.combineLatest(flowables) { arr ->
            (arr.toList() as List<List<Entity0>>).flatten()
        }
            .subscribe {
                entities = it.toList()
                Log.d("!!!!!", "Realm observed many Entity0")
                numberOfRows.value = it.size
            }
            .let { disposable.add(it) }

        val flowables1 = (0..50).map { idx ->
            realm!!.where(Entity1::class.java)
                .greaterThanOrEqualTo("id", 4000 + idx * 100)
                .findAllAsync()
                .asFlowable()
                .filter { it.isLoaded }
                .map { it.toList() }
        }
        Flowable.combineLatest(flowables1) { arr ->
            (arr.toList() as List<List<Entity1>>).flatten()
        }
            .subscribe {
                entities1 = it.toList()
                Log.d("!!!!!", "Realm observed many Entity1")
                numberOfRows1.value = it.size
            }
            .let { disposable.add(it) }
    }

    private fun deleteRealm() {
        lifecycleScope.launch(Dispatchers.IO) {
            val newRealm = Realm.getInstance(realm!!.configuration)
            newRealm.executeTransaction {
                it.deleteAll()
            }
            newRealm.close()
            Log.d("!!!!!", "Realm deleted")
        }
    }
}
