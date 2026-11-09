package com.example.adoptme.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
            alwaysShowLabel = false,
            label = {
                Text("Inicio", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            alwaysShowLabel = false,
            label = {
                Text("Centros", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            alwaysShowLabel = false,
            label = {
                Text("Chat", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            alwaysShowLabel = false,
            label = {
                Text("Avisos", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            alwaysShowLabel = false,
            label = {
                Text("Perfil", maxLines = 1, overflow = TextOverflow.Ellipsis)
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