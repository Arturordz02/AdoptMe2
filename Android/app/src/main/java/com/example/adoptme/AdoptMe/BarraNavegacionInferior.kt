package com.example.adoptme.AdoptMe

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.adoptme.R

@Composable
fun BarraNavegacionInferior(
    pantalla: String,
    cambiarPantalla: (String) -> Unit,
    mostrarAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {

    NavigationBar(
        modifier = modifier.fillMaxWidth()
    ) {

        NavigationBarItem(
            selected = pantalla == "home",
            onClick = {
                cambiarPantalla("home")
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "Inicio"
                )
            },
            label = {
                Text("Inicio")
            }
        )

        NavigationBarItem(
            selected = pantalla == "centros",
            onClick = {
                cambiarPantalla("centros")
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_center),
                    contentDescription = "Centros"
                )
            },
            label = {
                Text("Centros")
            }
        )

        NavigationBarItem(
            selected = pantalla == "chat",
            onClick = {
                cambiarPantalla("chat")
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chat),
                    contentDescription = "Chat"
                )
            },
            label = {
                Text("Chat")
            }
        )

        NavigationBarItem(
            selected = pantalla == "notifications",
            onClick = {
                cambiarPantalla("notifications")
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notifications),
                    contentDescription = "Avisos"
                )
            },
            label = {
                Text("Avisos")
            }
        )

        NavigationBarItem(
            selected = pantalla == "profile",
            onClick = {
                cambiarPantalla("profile")
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = "Perfil"
                )
            },
            label = {
                Text("Perfil")
            }
        )

        if (mostrarAdmin) {

            NavigationBarItem(
                selected = pantalla == "admin",
                onClick = {
                    cambiarPantalla("admin")
                },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Admin"
                    )
                },
                label = {
                    Text("Admin")
                }
            )
        }
    }
}