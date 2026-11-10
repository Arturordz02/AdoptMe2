package com.example.adoptme.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.adoptme.R

@Composable
fun BarraNavegacionInferior(
    pantalla: String,
    cambiarPantalla: (String) -> Unit,
    mostrarAdmin: Boolean = false,
    conteoNotificacionesNoLeidas: Int = 0,
    conteoMensajesNoLeidos: Int = 0,
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
                BadgedBox(
                    badge = {
                        if (conteoMensajesNoLeidos > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                val textoBadge = if (conteoMensajesNoLeidos > 99) "99+" else conteoMensajesNoLeidos.toString()
                                Text(text = textoBadge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chat),
                        contentDescription = "Chat"
                    )
                }
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
                BadgedBox(
                    badge = {
                        if (conteoNotificacionesNoLeidas > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                val textoBadge = if (conteoNotificacionesNoLeidas > 99) "99+" else conteoNotificacionesNoLeidas.toString()
                                Text(text = textoBadge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notifications),
                        contentDescription = "Avisos"
                    )
                }
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
