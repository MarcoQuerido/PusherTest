package pt.ulp.pushertest

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.userName)
        findViewById<Button>(R.id.enterButton).setOnClickListener {
            if (username.text.isNotEmpty()){
                val intent = Intent(this@LoginActivity,MainActivity::class.java)
                intent.putExtra("username",username.text.toString())
                startActivity(intent)
            }
        }
    }
}